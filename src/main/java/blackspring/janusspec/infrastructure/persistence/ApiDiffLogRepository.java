package blackspring.janusspec.infrastructure.persistence;

import blackspring.janusspec.domain.ApiDiffLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApiDiffLogRepository extends JpaRepository<ApiDiffLog, Long> {
    
    // 최신순으로 전체 조회
    Page<ApiDiffLog> findAllByOrderByIdDesc(Pageable pageable);
    
    // 특정 서비스의 변경 이력 조회
    @Query("SELECT d FROM ApiDiffLog d WHERE d.newVersion.serviceName = :serviceName ORDER BY d.id DESC")
    Page<ApiDiffLog> findByServiceNameOrderByIdDesc(@Param("serviceName") String serviceName, Pageable pageable);
    
    // 최근 N개 조회
    List<ApiDiffLog> findTop10ByOrderByIdDesc();
}

