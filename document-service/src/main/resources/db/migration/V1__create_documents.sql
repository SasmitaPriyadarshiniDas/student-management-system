CREATE TABLE documents (
    id               UUID PRIMARY KEY,
    student_id       UUID         NOT NULL,
    document_type    VARCHAR(30)  NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    content_type     VARCHAR(100) NOT NULL,
    size_bytes       BIGINT       NOT NULL,
    blob_name        VARCHAR(500) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    rejection_reason VARCHAR(500),
    verified_by      UUID,
    uploaded_at      TIMESTAMPTZ  NOT NULL,
    verified_at      TIMESTAMPTZ
);

CREATE INDEX idx_documents_student ON documents (student_id, uploaded_at DESC);
CREATE INDEX idx_documents_status ON documents (status);
