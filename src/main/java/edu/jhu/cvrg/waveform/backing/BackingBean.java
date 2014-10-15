package edu.jhu.cvrg.waveform.backing;

import org.apache.log4j.Logger;

public class BackingBean {

	protected Logger getLog(){
		return Logger.getLogger(this.getClass());
	}
}
