package com.fitlog.fitlogv2server.support;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.entity.Provider;
import com.fitlog.fitlogv2server.domain.member.entity.Role;
import com.fitlog.fitlogv2server.domain.member.repository.MemberRepository;
import com.fitlog.fitlogv2server.domain.workout.entity.Workout;
import com.fitlog.fitlogv2server.domain.workout.repository.WorkoutRepository;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.domain.workoutprogram.repository.WorkoutProgramRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 통합 테스트용 데이터 생성. 테스트끼리 DB를 공유하므로 매번 고유한 회원을 만들어 격리한다.
 */
@Component
public class TestFixtures {

    private final MemberRepository memberRepository;
    private final WorkoutProgramRepository workoutProgramRepository;
    private final WorkoutRepository workoutRepository;

    public TestFixtures(MemberRepository memberRepository,
                        WorkoutProgramRepository workoutProgramRepository,
                        WorkoutRepository workoutRepository) {
        this.memberRepository = memberRepository;
        this.workoutProgramRepository = workoutProgramRepository;
        this.workoutRepository = workoutRepository;
    }

    public Member member() {
        String unique = UUID.randomUUID().toString();
        return memberRepository.save(Member.builder()
                .email(unique + "@example.com")
                .nickname("user-" + unique.substring(0, 8))
                .provider(Provider.GOOGLE)
                .providerId(unique)
                .role(Role.USER)
                .build());
    }

    public WorkoutProgram emptyProgram(Member owner) {
        return workoutProgramRepository.save(WorkoutProgram.builder()
                .member(owner)
                .name("프로그램")
                .description("")
                .build());
    }

    /** 시드된 공용 운동 중 서로 다른 n개 */
    public List<Workout> publicWorkouts(int count) {
        return workoutRepository.findAllByMemberIdOrMemberIsNull(-1L).stream()
                .filter(w -> w.getMember() == null)
                .limit(count)
                .toList();
    }
}
