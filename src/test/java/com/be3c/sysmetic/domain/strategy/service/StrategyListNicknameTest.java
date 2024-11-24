package com.be3c.sysmetic.domain.strategy.service;

import com.be3c.sysmetic.domain.member.entity.Member;
import com.be3c.sysmetic.domain.member.repository.MemberRepository;
import com.be3c.sysmetic.domain.strategy.dto.TraderNicknameListDto;
import com.be3c.sysmetic.domain.strategy.entity.Method;
import com.be3c.sysmetic.domain.strategy.entity.Strategy;
import com.be3c.sysmetic.domain.strategy.repository.MethodRepository;
import com.be3c.sysmetic.domain.strategy.repository.StrategyListRepository;
import com.be3c.sysmetic.global.common.response.PageResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "/application-test.properties")
public class StrategyListNicknameTest {

    @Autowired
    StrategyListService strategyListService;

    // saveMember(), saveMethod(), deleteAll() 위함
    @Autowired
    StrategyListRepository strategyListRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MethodRepository methodRepository;
    @Autowired
    EntityManager em;

    @BeforeEach
    public void init() {
        em.createNativeQuery("ALTER TABLE sysmetic.strategy AUTO_INCREMENT = 1")
                .executeUpdate();
        // strategyListRepository 데이터 모두 삭제
        strategyListRepository.deleteAll();
        saveMember("홍길동");
        saveMethod();
    }

    @Test
    @DisplayName("트레이더 닉네임으로 검색")
    @Transactional
    @Rollback(false)
    public void findByTraderNickname() {
        // before : 현재 데이터베이스 비우기
        strategyListRepository.deleteAll();
        assertTrue(strategyListRepository.findAll().isEmpty());

        // 전략 수 난수는 [1, 100]
        int randomStrategyNum = (int) (Math.random() * 100 + 1);
        // int randomStrategyNum = 9;
        System.out.println("randomStrategyNum = " + randomStrategyNum);

        // 트레이더 저장
        saveMember("여의도");
        saveMember("여의도전략가");

        // 트레이더 수만큼 전략 생성 후 저장
        for (int i=0; i < randomStrategyNum; i++) {
            Strategy s = Strategy.builder()
                    .trader(i % 2 == 0 ? getTrader("여의도") : getTrader("여의도전략가"))
                    .method(getMethod())
                    .statusCode("ST001")
                    .name("전략" + (i+1))
                    .cycle('P')
                    .content("전략" + (i + 1) + " 소개 내용")
                    .followerCount((long) (Math.random() * 100))
                    .accumProfitLossRate(Math.random() * 100)
                    .build();
            em.persist(s);
            em.flush();
            em.clear();
        }

        // 페이지 하나
        PageResponse<TraderNicknameListDto> page = strategyListService.findTraderNickname("여의도", 0);
        assertNotNull(page);
        assertEquals(page.getPageSize(), 10);
        long maxFollowerCount = page.getContent().get(0).getTotalFollow();

        for (int i=0; i < page.getContent().size(); i++) {
            assertTrue(page.getContent().get(i).getNickname().contains("여의도"));
            assertTrue(page.getContent().get(i).getTotalFollow() <= maxFollowerCount);
        }
    }

    @Test
    @DisplayName("특정 닉네임을 가진 트레이더 목록 전부 조회")
    @Transactional
    @Rollback(value = false)
    public void findAllByTraderNickname() {
        // before : 현재 데이터베이스 비우기
        strategyListRepository.deleteAll();
        memberRepository.deleteAll();
        assertTrue(strategyListRepository.findAll().isEmpty());

        // 전략 수 난수는 [1, 100]
        int randomStrategyNum = (int) (Math.random() * 100 + 1);
        // int randomStrategyNum = 9;
        System.out.println("randomStrategyNum = " + randomStrategyNum);

        // 트레이더 저장
        saveMember("여의도부자");
        saveMember("여의도전략가");

        // 트레이더 수만큼 전략 생성 후 저장
        for (int i=0; i < randomStrategyNum-10; i++) {
            Strategy s = Strategy.builder()
                    .trader(getTrader("여의도부자"))
                    .method(getMethod())
                    .statusCode("ST001")
                    .name("전략" + (i+1))
                    .cycle('P')
                    .content("전략" + (i + 1) + " 소개 내용")
                    .followerCount((long) (Math.random() * 100))
                    .accumProfitLossRate(Math.random() * 100)
                    .build();
            em.persist(s);
            em.flush();
            em.clear();
        }

        for (int i=randomStrategyNum-10; i < randomStrategyNum; i++) {
            Strategy s = Strategy.builder()
                    .trader(getTrader("여의도전략가"))
                    .method(getMethod())
                    .statusCode("ST001")
                    .name("전략" + (i+1))
                    .cycle('P')
                    .content("전략" + (i + 1) + " 소개 내용")
                    .followerCount((long) (Math.random() * 100))
                    .accumProfitLossRate(Math.random() * 100)
                    .build();
            em.persist(s);
            em.flush();
            em.clear();
        }

        // 전체 페이지 계산
        int expectedTotalPage = (int) (Math.ceil(randomStrategyNum / 10.0));
        int actualTotalPage = (int) strategyListService.getTotalPageNumber("ST001", 10);
        assertEquals(expectedTotalPage, actualTotalPage);

        // 전체 페이지 조회
        for (int i=0; i < (int) Math.ceil(expectedTotalPage / 2.0); i++) {
            PageResponse<TraderNicknameListDto> page = strategyListService.findTraderNickname("여의도", 0);
            assertEquals(page.getPageSize(), 10);
            assertNotNull(page);

            long maxFollowerCount = page.getContent().get(0).getTotalFollow();

            for (int j=0; j < page.getContent().size(); j++) {
                assertTrue(page.getContent().get(j).getNickname().contains("여의도"));
                assertTrue(page.getContent().get(j).getTotalFollow() <= maxFollowerCount);
                System.out.println("닉네임 = " + page.getContent().get(j).getNickname() + ", followerCount = " + page.getContent().get(j).getTotalFollow());
            }
            System.out.println("=====" + i + "======");
        }
    }



    @Test
    @DisplayName("특정 닉네임을 가진 트레이더 없음")
    @Transactional
    @Rollback(value = false)
    public void failToFindTrader() {
        // before : 현재 데이터베이스 비우기
        strategyListRepository.deleteAll();
        assertTrue(strategyListRepository.findAll().isEmpty());

        saveMember("10층에도살아요");

        Strategy s = Strategy.builder()
                .trader(getTrader("10층에도살아요"))
                .method(getMethod())
                .statusCode("ST001")
                .name("전략" + ("김삼성"))
                .cycle('P')
                .content("전략 소개 내용")
                .followerCount((long) (Math.random() * 100))
                .accumProfitLossRate(Math.random() * 100)
                .build();
        em.persist(s);
        em.flush();
        em.clear();


        PageResponse<TraderNicknameListDto> page = strategyListService.findTraderNickname("강남", 0);
        assertTrue(page.getContent().isEmpty());
    }


    void saveMember(String nickname) {
        // Member trader 생성
        Member trader = Member.builder()
                .roleCode("trader")
                .email("trader1@gmail.com")
                .password("1234")
                .name("김이박")
                .nickname(nickname)
                .phoneNumber("01012341234")
                .usingStatusCode("using status code")
                .totalFollow(0)
                .totalStrategyCount(0)
                .receiveInfoConsent("Yes")
                .infoConsentDate(LocalDateTime.now())
                .receiveMarketingConsent("NO")
                .marketingConsentDate(LocalDateTime.now())
                .build();
        // trader 저장
        memberRepository.save(trader);
    }

    void saveMethod(){
        // Method 객체 생성
        Method method = Method.builder()
                .name("Manual")
                .statusCode("MS001")
                .build();
        // method 저장
        methodRepository.save(method);
    }

    Member getTrader() {
        return memberRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("트레이더가 없습니다."));
    }

    Member getTrader(String nickname) {
        return memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new NoSuchElementException("해당 트레이더가 없습니다."));
    }

    Method getMethod(){
        return methodRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("매매방식이 없습니다."));
    }
}
