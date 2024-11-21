package ampath.co.ke.amrs_kenyaemr.repositories;

import ampath.co.ke.amrs_kenyaemr.models.AMRSEnrollments;
import ampath.co.ke.amrs_kenyaemr.models.AMRSHIVEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AMRSHIVEnrollmentRepository extends JpaRepository<AMRSHIVEnrollment, Long> {
    AMRSHIVEnrollment findById(int pid);
    List<AMRSHIVEnrollment> findByPersonId(String pid);
    List<AMRSHIVEnrollment> findByUUID(String uuid);
    List<AMRSHIVEnrollment> findAll();
    // List< AMRSHIVEnrollment> findByUUIDAndParentlocationUUID(String uuid,String location);
    List<AMRSHIVEnrollment> findFirstByOrderByIdDesc();
}
