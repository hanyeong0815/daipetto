package koh.portfolio.springapi.infrastructure.persistence.user;

import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper extends DomainEntityMapper<User, UserEntity> {
}
