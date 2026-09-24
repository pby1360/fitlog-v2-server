package com.fitlog.fitlogv2server.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

// 시각은 시간대와 무관한 Instant(timestamptz)로 저장한다. 화면용 날짜는 AppTimeZone.KST 로 변환해 만든다.
@Getter
@MappedSuperclass // JPA 엔티티 클래스들이 이 클래스를 상속할 경우 필드(createdAt, updatedAt)도 칼럼으로 인식
@EntityListeners(AuditingEntityListener.class) // Auditing 기능 포함
public abstract class BaseTimeEntity {

    @CreatedDate // 엔티티 생성 시 시간 자동 저장
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate // 엔티티 값 변경 시 시간 자동 저장
    private Instant updatedAt;
}
