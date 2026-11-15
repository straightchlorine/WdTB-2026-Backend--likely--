package pl.wsb.likely.market.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.wsb.likely.market.entity.Resolution;

import java.util.Optional;

/** Question has no field pointing to its resolution, so it is read from here (one per question). */
public interface ResolutionRepository extends JpaRepository<Resolution, Long> {

    Optional<Resolution> findByQuestion_Id(Long questionId);
}
