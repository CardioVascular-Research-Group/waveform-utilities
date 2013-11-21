package edu.jhu.cvrg.waveform.model;

public enum PhysionetMethods{
	
	ANN2RR("ann2rr", "ann2rrWrapperType2"),
	NGUESS("nguess", "nguessWrapperType2"),
	PNNLIST("pnnlist/pNNx", "pnnlistWrapperType2"),
	RDSAMP("rdsamp", "rdsampWrapperType2"),
	SIGAAMP("sigamp", "sigampWrapperType2"),
	SQRS("sqrs", "sqrsWrapperType2"),
	TACH("tach", "tachWrapperType2"),
	WQRS("wqrs", "wqrsWrapperType2"),
	WRSAMP("wrsamp", "wrsampWrapperType2"), 
	CHESNOKOV("QT Screening", "chesnokovWrapperType2");
	
	private String name;
	private String omeName; 
	
	PhysionetMethods(String name, String omeName){
		this.omeName = omeName;
		this.name = name;
	}
	
	public String getOmeName(){
		return omeName;
	}

	public String getName() {
		return name;
	}
	
	public static PhysionetMethods getMethodByName(String name){
		
		for (PhysionetMethods m : values()) {
			if(m.getName().equals(name) || m.toString().equals(name)){
				return m;
			}
		}
		return null;
	}
}