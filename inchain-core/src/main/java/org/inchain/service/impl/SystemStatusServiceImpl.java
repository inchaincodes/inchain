package org.inchain.service.impl;

import org.inchain.core.DataSynchronizeHandler;
import org.inchain.service.SystemStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统状态服务
 * @author ln
 *
 */
@Service
public class SystemStatusServiceImpl implements SystemStatusService {

	private int status;
	
	@Autowired
	private DataSynchronizeHandler dataSynchronizeHandler;
	
	@Override
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public boolean isDataSynchronize() {
		if(dataSynchronizeHandler.hasComplete()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean isDataReset() {
		return status == DATA_RESET;
	}
}
