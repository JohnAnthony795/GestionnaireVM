package org.ctlv.proxmox.manager;

import org.ctlv.proxmox.api.ProxmoxAPI;
import org.ctlv.proxmox.generator.Generator;

public class ManagerMain {
	
	public static void main(String[] args) throws Exception {

		ProxmoxAPI api = new ProxmoxAPI();
		Object LOCK = new Object();

		// On crée et lance les instances du générateur de CT et du moniteur
		Monitor monitor = new Monitor(api, new Analyzer(api, new Controller(api)), LOCK);
		new Thread(monitor).start();

		Generator.run(api, LOCK);
		//Generator.stopCTs();
	}

}