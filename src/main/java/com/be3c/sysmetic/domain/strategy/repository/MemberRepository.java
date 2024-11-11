package com.be3c.sysmetic.domain.strategy.repository;

import com.be3c.sysmetic.domain.member.entity.Member;
import com.be3c.sysmetic.domain.strategy.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// TODO 추후 회원 파트와 머지 후 파일 삭제 예정

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 임시
    Member findOne(Long id);
}
