package koh.portfolio.springapi.infrastructure.persistence.auth;

import koh.portfolio.springapi.domain.auth.model.RefreshToken;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface RefreshTokenMapper extends DomainEntityMapper<RefreshToken, RefreshTokenEntity> {
}
