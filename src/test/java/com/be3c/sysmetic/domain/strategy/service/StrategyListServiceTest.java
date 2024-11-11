package com.be3c.sysmetic.domain.strategy.service;

import com.be3c.sysmetic.domain.member.entity.Member;
import com.be3c.sysmetic.domain.member.repository.MemberRepository;
import com.be3c.sysmetic.domain.strategy.dto.StrategyListDto;
import com.be3c.sysmetic.domain.strategy.dto.TraderListDto;
import com.be3c.sysmetic.domain.strategy.entity.Method;
import com.be3c.sysmetic.domain.strategy.entity.Strategy;
import com.be3c.sysmetic.domain.strategy.repository.MethodRepository;
import com.be3c.sysmetic.domain.strategy.repository.StrategyListRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "/application-test.properties")
public class StrategyListServiceTest {

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
        em.createNativeQuery("ALTER TABLE sysmetictest.strategy AUTO_INCREMENT = 1")
                .executeUpdate();
        // strategyListRepository 데이터 모두 삭제
        strategyListRepository.deleteAll();
        saveMember("홍길동");
        saveMethod();
    }

    @Test
    @DisplayName("첫 번째 페이지 조회 테스트")
    @Transactional
    @Rollback(false)
    public void getFirstPageTest() {
        // before : 현재 데이터베이스 비우기
        strategyListRepository.deleteAll();
        assertTrue(strategyListRepository.findAll().isEmpty());

        // 전략 수 난수는 [1, 100]
        int randomStrategyNum = (int) (Math.random() * 100) + 1;
        System.out.println("randomStrategyNum = " + randomStrategyNum);

        // 난수만큼 순차적으로 전략 생성
        for (int i = 0; i < randomStrategyNum; i++) {
            Strategy s = Strategy.builder()
                    .trader(getTrader())
                    .method(getMethod())
                    .statusCode("ST001")
                    .name("전략" + (i + 1))
                    .cycle('P')
                    .minOperationAmount(100.0)
                    .content("전략" + (i + 1) + " 소개 내용")
                    .followerCount((long) (Math.random() * 100))
                    .accumProfitRate(Math.random() * 100)
                    .createdBy((long) randomStrategyNum)
                    .modifiedBy((long) randomStrategyNum)
                    .build();
            em.persist(s);
            em.flush();
            em.clear();
        }

        // 전략 첫 번째 페이지 가져오기
        Page<StrategyListDto> firstPage = strategyListService.findStrategyPage(0);
        assertNotNull(firstPage);
        assertEquals(10, firstPage.getSize());
        assertTrue(firstPage.hasContent());

        // 첫 번째 페이지 전략 반복 검증
        for (StrategyListDto dto : firstPage) {
            assertNotNull(dto.getName());
            assertNotNull(dto.getStock());
            assertNotNull(dto.getTraderNickname());
            assertNotNull(dto.getCycle());
            assertNotNull(dto.getAccumProfitRate());
        }

        // 첫 페이지 첫 번째 값이 제일 큰 accumProfitRate 가져야 함
        for (StrategyListDto dto : firstPage) {
            assertTrue(firstPage.getContent().get(0).getAccumProfitRate() >= dto.getAccumProfitRate());
            System.out.println("dto.getAccumProfitRate() = " + dto.getAccumProfitRate());
        }
    }


    @Test
    @DisplayName("전략 목록 전체 페이지 수")
    @Transactional
    @Rollback(false)
    public void getAllStrategyPageNum() {
        // before : 현재 데이터베이스 비우기
        strategyListRepository.deleteAll();
        assertTrue(strategyListRepository.findAll().isEmpty());

        // 전략 수 난수는 [1, 100]
        int randomStrategyNum = (int) (Math.random() * 100) + 1;
        // int randomStrategyNum = 9;
        System.out.println("randomStrategyNum = " + randomStrategyNum);

        // 난수만큼 순차적으로 전략 생성
        for (int i = 0; i < randomStrategyNum; i++) {
            Strategy s = Strategy.builder()
                    .trader(getTrader())
                    .method(getMethod())
                    .statusCode("ST001")
                    .name("전략" + (i + 1))
                    .cycle('P')
                    .minOperationAmount(100.0)
                    .content("전략" + (i + 1) + " 소개 내용")
                    .followerCount((long) (Math.random() * 100))
                    .accumProfitRate(Math.random() * 100)
                    .createdBy((long) randomStrategyNum)
                    .modifiedBy((long) randomStrategyNum)
                    .build();

            // 저장할 때는 하나씩 등록하니까 StrategyRepository 사용해서 하나씩 등록
            em.persist(s);
            em.flush();
            em.clear();
        }

        // 전체 페이지 수 확인
        int expectedTotalPage = (int) Math.ceil(randomStrategyNum / 10.0);
        int actualTotalPage = strategyListService.getTotalPageNumber("ST001", 10);
        System.out.println("expectedTotalPage = " + expectedTotalPage);
        System.out.println("actualTotalPage = " + actualTotalPage);
    }


    @Test
    @DisplayName("특정 페이지 조회")
    @Transactional
    @Rollback(false)
    public void getSelectedPageTest() {
        // before : 현재 데이터베이스 비우기
        strategyListRepository.deleteAll();
        assertTrue(strategyListRepository.findAll().isEmpty());

        // 전략 수 난수는 [1, 100]
        int randomStrategyNum = (int) (Math.random() * 100 + 1);
        // int randomStrategyNum = 9;
        System.out.println("randomStrategyNum = " + randomStrategyNum);

        // 난수만큼 순차적으로 전략 생성
        for (int i = 0; i < randomStrategyNum; i++) {
            Strategy s = Strategy.builder()
                    .trader(getTrader())
                    .method(getMethod())
                    .statusCode("ST001")
                    .name("전략" + (i + 1))
                    .cycle('P')
                    .minOperationAmount(100.0)
                    .content("전략" + (i + 1) + " 소개 내용")
                    .followerCount((long) (Math.random() * 100))
                    .accumProfitRate(Math.random() * 100)
                    .createdBy((long) randomStrategyNum)
                    .modifiedBy((long) randomStrategyNum)
                    .build();

            // 저장할 때는 하나씩 등록하니까 StrategyRepository 사용해서 하나씩 등록
            em.persist(s);
            em.flush();
            em.clear();
        }

        // 전체 페이지 계산
        int totalPage = (int) Math.ceil(randomStrategyNum / 10.0);
        // 랜덤 페이지 선택
        int randomPage = (int) (Math.random() * totalPage);
        System.out.println("randomPage = " + randomPage);

        // 특정 페이지 가져오기
        Page<StrategyListDto> selectPage = strategyListService.findStrategyPage(randomPage);
        assertFalse(selectPage.isEmpty(), "선택한 페이지에 데이터 없음.");
        double maxProfitRate = selectPage.getContent().get(0).getAccumProfitRate();

        // 가져온 페이지도 수익률 순으로 정렬되어야 함
        for (StrategyListDto s : selectPage) {
            assertTrue(maxProfitRate >= s.getAccumProfitRate());
            System.out.println("s.getAccumProfitRate() = " + s.getAccumProfitRate());
        }
    }


    @Test
    @DisplayName("전략 목록 전체 페이지 조회")
    @Transactional
    @Rollback(false)
    public void getAllStrategyPages() {
        // before : 현재 데이터베이스 비우기
        strategyListRepository.deleteAll();
        assertTrue(strategyListRepository.findAll().isEmpty());

        // 전략 수 난수는 [1, 100]
        int randomStrategyNum = (int) (Math.random() * 100 + 1);
        // int randomStrategyNum = 9;
        System.out.println("randomStrategyNum = " + randomStrategyNum);

        // 난수만큼 순차적으로 전략 생성
        for (int i = 0; i < randomStrategyNum; i++) {
            Strategy s = Strategy.builder()
                    .trader(getTrader())
                    .method(getMethod())
                    .statusCode("ST001")
                    .name("전략" + (i + 1))
                    .cycle('P')
                    .minOperationAmount(100.0)
                    .content("전략" + (i + 1) + " 소개 내용")
                    .followerCount((long) (Math.random() * 100))
                    .accumProfitRate(Math.random() * 100)
                    .createdBy((long) randomStrategyNum)
                    .modifiedBy((long) randomStrategyNum)
                    .build();

            // 저장할 때는 하나씩 등록하니까 StrategyRepository 사용해서 하나씩 등록
            em.persist(s);
            em.flush();
            em.clear();
        }

        // 전체 페이지 계산
        int expectedTotalPage = (int) Math.ceil(randomStrategyNum / 10.0);
        int actualTotalPage = strategyListService.getTotalPageNumber("ST001", 10);
        assertEquals(expectedTotalPage, actualTotalPage);

        for (int i=0; i < actualTotalPage; i++) {
            Page<StrategyListDto> page = strategyListService.findStrategyPage(i);
            assertNotNull(page);
            assertTrue(page.hasContent());

            for (int j=0; j < page.getContent().size(); j++) {
                assertTrue(page.getContent().get(0).getAccumProfitRate() >= page.getContent().get(j).getAccumProfitRate());
                System.out.println("AccumProfitRate = " + page.getContent().get(j).getAccumProfitRate());
            }
            System.out.println("=====================");
        }
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
                    .minOperationAmount(100.0)
                    .content("전략" + (i + 1) + " 소개 내용")
                    .followerCount((long) (Math.random() * 100))
                    .accumProfitRate(Math.random() * 100)
                    .createdBy(1L)
                    .modifiedBy(1L)
                    .build();
            em.persist(s);
            em.flush();
            em.clear();
        }

        // 페이지 하나
        Page<TraderListDto> page = strategyListService.findByTrader("여의도");
        assertNotNull(page);
        assertTrue(page.hasContent());
        assertEquals(page.getSize(), 10);
        long maxFollowerCount = page.getContent().get(0).getFollowerCount();

        for (TraderListDto t : page) {
            assertTrue(t.getNickname().contains("여의도"));
            assertTrue(t.getFollowerCount() <= maxFollowerCount);
            System.out.println("id = " + t.getTraderId() + ", followerCount() = " + t.getFollowerCount());
        }
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
                .receiveInfoConsent("Yes")
                .infoConsentDate(LocalDateTime.now())
                .receiveMarketingConsent("NO")
                .marketingConsentDate(LocalDateTime.now())
                .createdBy(1L)
                .createdDate(LocalDateTime.now())
                .modifiedBy(1L)
                .modifiedDate(LocalDateTime.now())
                .build();
        // trader 저장
        memberRepository.save(trader);
    }

    void saveMethod(){
        // Method 객체 생성
        Method method = Method.builder()
                .name("Manual")
                .statusCode("MS001")
                .createdBy(1L)
                .modifiedBy(1L)
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