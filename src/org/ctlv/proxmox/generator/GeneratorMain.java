package org.ctlv.proxmox.generator;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.security.auth.login.LoginException;

import org.ctlv.proxmox.api.Constants;
import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.api.data.LXC;
import org.json.JSONException;

public class GeneratorMain {

	static Random rndTime = new Random(new Date().getTime());

	public static int getNextEventPeriodic(int period) {
		return period;
	}

	public static int getNextEventUniform(int max) {
		return rndTime.nextInt(max);
	}

	public static int getNextEventExponential(int inv_lambda) {
		float next = (float) (-Math.log(rndTime.nextFloat()) * inv_lambda);
		return (int) next;
	}

	public static void main(String[] args) throws InterruptedException, LoginException, JSONException, IOException {

		long baseID = Constants.CT_BASE_ID;
		String baseCt = Constants.CT_BASE_NAME;
		int lambda = (int) Constants.GENERATION_WAIT_TIME;

		Map<String, List<LXC>> myCTsPerServer = new HashMap<String, List<LXC>>();

		ProxmoxAPI api = new ProxmoxAPI();
		Random rndServer = new Random(new Date().getTime());
		Random rndRAM = new Random(new Date().getTime());

		long memAllowedOnServer1 = (long) (api.getNode(Constants.SERVER1).getMemory_total() * Constants.MAX_THRESHOLD);
		long memAllowedOnServer2 = (long) (api.getNode(Constants.SERVER2).getMemory_total() * Constants.MAX_THRESHOLD);

		while (true) {

			// 1. Calculer la quantité de RAM utilisée par mes CTs sur chaque serveur
			long memOnServer1 = 0;
			long memOnServer2 = 0;

			for (LXC ct : api.getCTs(Constants.SERVER1))
				if (ct.getName().contains(Constants.CT_BASE_NAME))
					memOnServer1 += ct.getMem();

			for (LXC ct : api.getCTs(Constants.SERVER2))
				if (ct.getName().contains(Constants.CT_BASE_NAME))
					memOnServer2 += ct.getMem();

			// Mémoire autorisée sur chaque serveur
			float memRatioOnServer1 = memOnServer1 / memAllowedOnServer1;
			float memRatioOnServer2 = memOnServer2 / memAllowedOnServer2;

			if (memRatioOnServer1 < 1 && memRatioOnServer2 < 1) {

				// choisir un serveur aléatoirement avec les ratios spécifiés 66% vs 33%
				String serverName;
				if (rndServer.nextFloat() < Constants.CT_CREATION_RATIO_ON_SERVER1)
					serverName = Constants.SERVER1;
				else
					serverName = Constants.SERVER2;

				// création container
				api.createCT(serverName, Long.toString(baseID), baseCt + baseID % 100, Constants.RAM_SIZE[1]);
				baseID++;

				// planifier la prochaine cr�ation
				int timeToWait = getNextEventExponential(lambda); // par exemple une loi expo d'une moyenne de 30sec

				// attendre jusqu'au prochain évènement
				Thread.sleep(1000 * timeToWait);
				try {
					api.startCT(serverName, Long.toString(baseID - 1));
				} catch (Exception e) {
				}

			} else {
				System.out.println("Servers are loaded, waiting ...");
				Thread.sleep(Constants.GENERATION_WAIT_TIME * 1000);
			}
		}

	}

}
