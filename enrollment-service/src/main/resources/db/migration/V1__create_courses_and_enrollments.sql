CREATE TABLE courses (
    id             UUID PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL UNIQUE,
    title          VARCHAR(200) NOT NULL,
    description    VARCHAR(2000),
    capacity       INTEGER      NOT NULL CHECK (capacity > 0),
    enrolled_count INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_seats CHECK (enrolled_count >= 0 AND enrolled_count <= capacity)
);

CREATE TABLE enrollments (
    id           UUID PRIMARY KEY,
    student_id   UUID        NOT NULL,
    course_id    UUID        NOT NULL REFERENCES courses (id),
    status       VARCHAR(20) NOT NULL,
    enrolled_at  TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ
);

CREATE INDEX idx_enrollments_student ON enrollments (student_id, enrolled_at DESC);

-- A student can only hold one ACTIVE enrollment per course (cancelled ones don't count).
CREATE UNIQUE INDEX ux_active_enrollment ON enrollments (student_id, course_id) WHERE status = 'ACTIVE';
