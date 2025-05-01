package com.traffic.pointservice.service.v2;

import com.traffic.pointservice.domain.Point;
import com.traffic.pointservice.domain.PointBalance;
import com.traffic.pointservice.domain.PointType;
import com.traffic.pointservice.repository.PointBalanceRepository;
import com.traffic.pointservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PointRedisService {
    private static final String POINT_BALANCE_MAP = "point:balance";
    private static final String POINT_LOCK_PREFIX = "point:lock:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 3L;
    private final RedissonClient redissonClient;
    private final PointBalanceRepository pointBalanceRepository;
    private final PointRepository pointRepository;

    /*
        동작 흐름
        1. 분산 락 획득
        2. 캐시된 잔액 조회(데이터가 없다면 DB에서 조회)
        3. 포인트 잔액 증가
        4. DB 저장 및 캐시 업데이트
        5. 포인트 이력 저장
     */
    @Transactional
    public Point earnPoints(Long userId, Long amount, String description){
        // 분산 락
        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX+userId);
        try{
            // 락 획득 시도
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if(!locked){
                // 락 획득 실패
                throw new IllegalStateException("Point 관련 Lock 획득에 실패했습니다.");
            }


            // 포인트 잔액 증가
            PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                    .orElseGet(() -> PointBalance.builder()
                            .userId(userId)
                            .balance(0L)
                            .build());

            pointBalance.addBalance(amount);
            pointBalance = pointBalanceRepository.save(pointBalance);

            // 캐시 업데이트
            updateBalanceCache(userId, pointBalance.getBalance());

            // Point 이력 저장
            Point point = Point.builder()
                    .userId(userId)
                    .amount(amount)
                    .type(PointType.EARNED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();

            return pointRepository.save(point);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition was interrupted", e);
        }finally {
            // 락 해제
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
        
    }

    @Transactional
    public Point usePoints(Long userId, Long amount, String description){
        // 분산 락 획득
        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX+userId);

        try{
            // 락 획득
            boolean locked = lock.tryLock(LOCK_WAIT_TIME,LOCK_LEASE_TIME,TimeUnit.SECONDS);
            if(!locked){
                throw new IllegalStateException("Point-Use 관련 Lock 획득에 실패했습니다.");
            }

            // 캐시된 잔액 조회
            Long currentBalance = getBalanceFromCache(userId);
            if(currentBalance == null){
                currentBalance = getBalanceFromDB(userId);       // DB에서 데이터 가져옴
                // 캐시 업데이트
                updateBalanceCache(userId, currentBalance);
            }

            if(currentBalance < amount){
                // 잔액 부족
                throw new IllegalArgumentException("포인트 잔액 부족");
            }

            // 잔액 포인트 감소
            PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

            pointBalance.subtractBalance(amount);
            pointBalance = pointBalanceRepository.save(pointBalance);
            
            // 캐시 최신화
            updateBalanceCache(userId, pointBalance.getBalance());

            // Point 이력 저장
            Point point = Point.builder()
                    .userId(userId)
                    .amount(amount)
                    .type(PointType.USED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();

            return pointRepository.save(point);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition was interrupted", e);
        }finally {
            // 락 해제
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }


    @Transactional
    public Point cancelPoints(Long pointId, String description){
        // 취소하고 싶은 포인트 이력 조회
        Point cancelPoint = pointRepository.findById(pointId)
                .orElseThrow(() -> new IllegalArgumentException("해당 포인트 이력이 없습니다"));

        Long userId = cancelPoint.getUserId();
        // 분산 락 획득
        RLock lock = redissonClient.getLock(POINT_LOCK_PREFIX+userId);

        try{
            boolean locked = lock.tryLock(LOCK_WAIT_TIME,LOCK_LEASE_TIME,TimeUnit.SECONDS);
            if(!locked){
                throw new IllegalStateException("Point-Cancel 관련 Lock 획득에 실패했습니다.");
            }

            if(cancelPoint.getType() == PointType.CANCELED){
                // 이미 취소된 포인트 사용건
                throw new IllegalArgumentException("이미 취소된 포인트 사용 건 입니다.");
            }

            // 포인트 환불
            PointBalance pointBalance = cancelPoint.getPointBalance();
            if(cancelPoint.getType() == PointType.EARNED){      // 포인트 적립된 건에 대해 취소할때
                pointBalance.subtractBalance(cancelPoint.getAmount());      // 현재 잔액에서 값 빼줌
            }else{              // 포인트 사용한 건에 대해 취소할때
                pointBalance.addBalance(cancelPoint.getAmount());
            }

            // DB 최신화
            pointBalance = pointBalanceRepository.save(pointBalance);

            // 캐시 최신화
            updateBalanceCache(userId, pointBalance.getBalance());

            // Point 취소 이력 저장
            Point point = Point.builder()
                    .userId(userId)
                    .amount(cancelPoint.getAmount())
                    .type(PointType.CANCELED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();

            return pointRepository.save(point);

        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition was interrupted", e);
        }finally {
            // 락 해제
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    // 유저별 포인트 잔액 찾기
    @Transactional(readOnly = true)
    public Long getBalance(Long userId){
        Long cacheBalance = getBalanceFromCache(userId);
        if(cacheBalance != null){
            return cacheBalance;
        }

        Long dbBalance = getBalanceFromDB(userId);
        updateBalanceCache(userId, dbBalance);

        return dbBalance;
    }


    private void updateBalanceCache(Long userId, Long currentBalance) {
        RMap<String, Long> balanceMap = redissonClient.getMap(POINT_BALANCE_MAP);
        balanceMap.put(String.valueOf(userId), currentBalance);
    }

    private Long getBalanceFromDB(Long userId) {
        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance :: getBalance)
                .orElse(0L);
    }

    private Long getBalanceFromCache(Long userId) {
        RMap<String,Long> balanceMap = redissonClient.getMap(POINT_BALANCE_MAP);
        return balanceMap.get(String.valueOf(userId));
    }
}
