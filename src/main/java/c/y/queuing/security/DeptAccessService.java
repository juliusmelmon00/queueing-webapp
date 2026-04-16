package c.y.queuing.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("deptAccessService")
public class DeptAccessService {
	private final DeptRoleHelper deptRoleHelper;
	
	public DeptAccessService(DeptRoleHelper deptRoleHelper) {
		this.deptRoleHelper = deptRoleHelper;
	}
	
	public boolean canAccessDept(Authentication auth, String dept) {
		if (auth == null || !auth.isAuthenticated() || dept == null) {
			return false;
		}
		//admin should use /admin/config, not/dept/**
		if (deptRoleHelper.isAdmin(auth)) {
			return true;
		}
		String userDept = deptRoleHelper.resolveDept(auth);
		if (userDept == null) {
			return false;
		}
		return userDept.equalsIgnoreCase(dept);
	}

}
