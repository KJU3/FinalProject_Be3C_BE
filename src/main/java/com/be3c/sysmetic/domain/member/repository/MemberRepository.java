package com.be3c.sysmetic.domain.member.repository;

import com.be3c.sysmetic.domain.member.dto.MemberGetResponseDto;
import com.be3c.sysmetic.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT count(m) FROM Member m WHERE m.roleCode = 'USER'")
    Long countUser();

    @Query("SELECT count(m) FROM Member m WHERE m.roleCode = 'TRADER'")
    Long countTrader();

    @Query("SELECT count(m) FROM Member m WHERE m.roleCode IN ('USER_MANAGER', 'TRADER_MANAGER')")
    Long countManager();

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByIdAndUsingStatusCode(Long id, String UsingStatusCode);

    Optional<Member> findByNickname(String nickname);

    @Modifying
    @Query("UPDATE Member m SET m.password = :newPassword WHERE m.email = :email")
    int updatePasswordByEmail(@Param("email") String email, @Param("newPassword") String newPassword);

    // 이름과 휴대번호로 이메일 찾기 (이메일이 여러 개 존재할 수 있어서 List로 반환)
    @Query(value = "SELECT m.email FROM Member m WHERE m.name = :name AND m.phoneNumber = :phoneNumber")
    List<String> findEmailByNameAndPhoneNumber(@Param("name") String name, @Param("phoneNumber") String phoneNumber);

    @Modifying
    @Query("UPDATE Member m SET m.roleCode = :roleCode WHERE m.id = :memberId")
    int updateRoleCode(@Param("memberId") Long memberId, @Param("roleCode") String roleCode);

    @Query(value = """
        SELECT new com.be3c.sysmetic.domain.member.dto.MemberGetResponseDto(
            m.id, m.roleCode, m.email, m.name, m.nickname, m.birth, m.phoneNumber
        )
        FROM Member m
        WHERE (
            (:role = 'ALL' AND m.roleCode IN ('RC001', 'RC002', 'RC003', 'RC004', 'USER', 'TRADER', 'USER_MANAGER', 'TRADER_MANAGER')) OR
            (:role = 'USER' AND m.roleCode IN ('RC001', 'USER')) OR
            (:role = 'TRADER' AND m.roleCode IN ('RC002', 'TRADER')) OR
            (:role = 'MANAGER' AND m.roleCode IN ('RC003', 'RC004', 'USER_MANAGER', 'TRADER_MANAGER'))
        )
        AND (
                (:searchType = 'NICKNAME' AND m.nickname LIKE CONCAT('%', :searchKeyword, '%')) OR
                (:searchType = 'EMAIL' AND m.email LIKE CONCAT('%', :searchKeyword, '%')) OR
                (:searchType = 'NAME' AND m.name LIKE CONCAT('%', :searchKeyword, '%')) OR
                (:searchType = 'PHONENUMBER' AND m.phoneNumber LIKE CONCAT('%', :searchKeyword, '%')) OR
                ((:searchType IS NULL OR :searchType = 'ALL') AND (
                    (:searchKeyword IS NULL OR m.email LIKE CONCAT('%', :searchKeyword, '%')) OR
                    (:searchKeyword IS NULL OR m.name LIKE CONCAT('%', :searchKeyword, '%')) OR
                    (:searchKeyword IS NULL OR m.nickname LIKE CONCAT('%', :searchKeyword, '%')) OR
                    (:searchKeyword IS NULL OR m.phoneNumber LIKE CONCAT('%', :searchKeyword, '%'))
                    )
                )
        )
        ORDER BY m.id DESC
    """
    )
    Page<MemberGetResponseDto> findMembers(
            @Param("role") String role,
            @Param("searchType") String searchType,
            @Param("searchKeyword") String searchKeyword,
            Pageable pageable
    );

    @Query("SELECT DISTINCT m FROM Member m " +
            "LEFT JOIN FETCH m.folders " +
            "LEFT JOIN FETCH m.strategies " +
            "LEFT JOIN FETCH m.replies " +
            "WHERE m.id = :memberId")
    Optional<Member> findMemberByIdWithStrategiesAndFolderAndReply(Long memberId);

    Optional<Member> findDistinctByNickname(String nickname);

    Optional<Member> findByPassword(String email);

    // 메인 페이지에서 사용!
    Long countAllByRoleCode(String roleCode);

}
