package com.sms.document.repository;

import com.sms.document.model.Document;
import com.sms.document.model.DocumentStatus;
import com.sms.document.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByStudentIdOrderByUploadedAtDesc(UUID studentId);

    @Query("select distinct d.type from Document d where d.studentId = :studentId and d.status = :status")
    List<DocumentType> findDistinctTypes(@Param("studentId") UUID studentId, @Param("status") DocumentStatus status);
}
