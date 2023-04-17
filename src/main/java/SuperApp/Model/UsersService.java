package SuperApp.Model;

import java.util.List;
import java.util.Optional;

public interface UsersService {
	
	public UserBoundary createUser(UserBoundary user);
	
	public Optional<UserBoundary> login(String userSuperApp, String userEmail);
	
	public UserBoundary updateUser(String userSuperApp, String userEmail, UserBoundary update);
	
	public List<UserBoundary> getAllUsers();

	public void deleteAllUsers();
}