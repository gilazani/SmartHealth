package superapp.dal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import superapp.Boundary.ObjectId;
import superapp.data.mainEntity.SuperAppObjectEntity;



public interface  SuperAppObjectRepository
        extends MongoRepository<SuperAppObjectEntity, ObjectId> {
    Page<SuperAppObjectEntity> findByAlias(String alias, Pageable pageable);
    Page<SuperAppObjectEntity> searchByType(String type, Pageable pageable);
}
