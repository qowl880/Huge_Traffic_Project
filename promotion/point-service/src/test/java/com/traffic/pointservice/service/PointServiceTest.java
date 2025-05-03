package com.traffic.pointservice.service;

import com.traffic.pointservice.domain.Point;
import com.traffic.pointservice.domain.PointBalance;
import com.traffic.pointservice.domain.PointType;
import com.traffic.pointservice.repository.PointBalanceRepository;
import com.traffic.pointservice.repository.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @Mock
    private PointBalanceRepository pointBalanceRepository;

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private PointService pointService;

    private Long userId;
    private Long amount;
    private String description;
    private PointBalance pointBalance;
    private Point point;

    @BeforeEach
    void setUp(){
        userId = 1L;
        amount = 1000L;
        description = "Test";

        pointBalance = PointBalance.builder()
                .userId(userId)
                .balance(1000L)
                .build();

        point = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.EARNED)
                .description(description)
                .balanceSnapshot(amount)
                .build();
    }


    @Test
    @DisplayName("포인트 적립 성공 테스트")
    void earnPointSuccess(){
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));     //  기존 포인트 조회 (반환값 : 값 존재)
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(answer -> answer.getArgument(0));   // 잔액 업데이트 모킹 (반환값 : 첫번째 인자값 반환 즉, PointBalance 반환)
        given(pointRepository.save(any(Point.class)))
                .willAnswer(answer -> {     // 포인트 이력 저장 모킹
                    Point savedPoint = answer.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when
        Point result = pointService.earnPoints(userId, amount, description);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class)); // 포인트 잔액 저장 메서드(save)가 한 번 호출되었는지 검증합니다
        verify(pointRepository, times(1)).save(any(Point.class));   // 포인트 이력 저장 메서드(save)가 한 번 호출되었는지 검증합니다.
    }


    @Test
    @DisplayName("포인트 사용 성공 테스트")
    void usePointFailure(){
        //given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(answer -> answer.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(answer -> {     // 포인트 이력 저장 모킹
                    Point savedPoint = answer.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when
        Point result = pointService.usePoints(userId, amount, description);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(PointType.USED);
        assertThat(result.getAmount()).isEqualTo(amount);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 사용 실패 - 잔액 부족")
    void usePointsInsufficientBalance(){
        //given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));

        // when, then
        assertThatThrownBy(() -> pointService.usePoints(userId, amount * 2, description))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트가 부족합니다.");
    }

    @Test
    @DisplayName("포인트 사용 실패 - 해당 유저 없음")
    void usePointsUserNotFound() {
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.usePoints(userId, amount, description))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 유저가 없습니다. :"+userId);
    }


}