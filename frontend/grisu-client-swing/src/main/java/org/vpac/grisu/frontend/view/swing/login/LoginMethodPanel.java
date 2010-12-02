package org.vpac.grisu.frontend.view.swing.login;

import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.frontend.control.login.LoginParams;

public interface LoginMethodPanel {

	public Exception getPossibleException();

	public ServiceInterface getServiceInterface();

	public void lockUI(boolean lock);

	public Thread login(LoginParams params);

	public boolean loginSuccessful();

}
