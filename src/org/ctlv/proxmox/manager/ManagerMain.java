package org.ctlv.proxmox.manager;

import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.generator.GeneratorMain;

public class ManagerMain {

	public static void main(String[] args) throws Exception {

		ProxmoxAPI api = new ProxmoxAPI();

		// On crée et lance les instances du générateur de CT et du moniteur
		Monitor monitor = new Monitor(api, new Analyzer(api, new Controller(api)));
		new Thread(monitor).start();

		GeneratorMain.main(null);
	}

}