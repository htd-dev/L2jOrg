package org.l2j.authserver.network.gameserver.packet.game2auth;

import org.l2j.authserver.controller.AuthController;

import java.nio.ByteBuffer;

public class PlayerLogout extends GameserverReadablePacket {
	
	private String account;

	public String getAccount()
	{
		return account;
	}

	@Override
	protected void readImpl() {
		account = readString();
	}

	@Override
	protected void runImpl()  {
		client.getGameServerInfo().removeAccount(account);
		AuthController.getInstance().removeAuthedClient(account);
	}
}