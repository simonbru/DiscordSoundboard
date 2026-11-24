package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends CrudRepository<User, String>, PagingAndSortingRepository<User, String> {
    User findOneByIdOrUsernameIgnoreCase(String id, String userName);
}
