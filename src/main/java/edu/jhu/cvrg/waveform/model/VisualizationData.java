package edu.jhu.cvrg.waveform.model;

import java.io.Serializable;

/** The raw ECG samples, in millivolts, for displaying the ecg chart in graphical form.
 * 
 * @author M.Shipway, W.Gertin
 *
 */
public class VisualizationData implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/** Id of the subject this data refers to **/
	String subjectID;
	
	/** Count of samples in ECG data. (rows)*/
	int ecgDataLength;
	
	/** Count of leads in ECG data. (columns)*/
	int ecgDataLeadCount = 3;

	String[] saLeadName;
	
	/** The raw ECG samples **/
	double[][] ecgData;
	
	/** Offset, in samples, from beginning of the ecg data set (SubjectData). Zero offset means first reading in data set.**/
	int offset;
	
	/**Number of samples to skip after each one returned. To adjust for graph resolution.**/
	int skippedSamples;
	
	/** duration of the ECG in milliseconds. **/
	int msDuration;
	
//****************************************	
	/** @return the Id of the subject this data refers to. 	 */
	public String getSubjectID() {
		return subjectID;
	}
	/** Set the Id of the subject this data refers to.
	 * @param subjectID the subjectID to set */
	public void setSubjectID(String subjectID) {
		this.subjectID = subjectID;
	}
	
	/**Get number of samples to skip after each one returned. To adjust for graph resolution.**/
	public int getSkippedSamples() {
		return skippedSamples;
	}
	/**Set number of samples to skip after each one returned. To adjust for graph resolution.**/
	public void setSkippedSamples(int skippedSamples) {
		this.skippedSamples = skippedSamples;
	}

	/** Get offset, in samples, from beginning of the ecg data set (SubjectData).**/
	public int getOffset() { return offset; }
	/** Set offset, in samples, from beginning of the ecg data set (SubjectData). **/
	public void setOffset(int offset) { this.offset = offset; }
	
	/** Get the count of leads in ECG data. (columns)*/
	public int getECGDataLeads() {return ecgDataLeadCount; }
	/** Set the count of leads in ECG data. (columns)*/
	public void setECGDataLeads(int ecgDataLeads) { this.ecgDataLeadCount = ecgDataLeads; }

	public String[] getSaLeadName() {
		return saLeadName;
	}
	public void setSaLeadName(String[] saLeadName) {
		this.saLeadName = saLeadName;
	}
	/** Get the count of samples in ECG data.(rows)*/
	public int getECGDataLength() {return ecgDataLength;}
	/** Get the count of samples in ECG data.(rows)*/
	public void setECGDataLength(int ecgDataLength) { this.ecgDataLength = ecgDataLength; }

	/** Get the duration of the ECG in milliseconds. 
	 * @return the msDuration
	 */
	public int getMsDuration() {
		return msDuration;
	}
	/** Set the duration of the ECG in milliseconds. 
	 * @param msDuration the msDuration to set
	 */
	public void setMsDuration(int msDuration) {
		this.msDuration = msDuration;
	}

	/** ECG samples in millivolts, one column per lead, one row per sample displayed. */
	public double[][] getECGData() {return ecgData;}
	public void setECGData(double[][] ecgData) {
		this.ecgData = ecgData;
	}
	
	/** ECG samples as String array, in millivolts, one column per lead, one row per sample displayed. <BR>
	 * The first column is sample time-stamp in milliseconds.<BR>
	 * Also adds a header row from the "this.saLeadName" array, set by VisualizationManager using serverUtil.guessLeadName(). 
	 * 
	 * @return
	 */
	public String[][] getECGDataStringArray() {
		int dRow = ecgData.length;
		int dCol = ecgData[0].length; // ecgDataLeads
		String[][] saECGData = new String[dRow+1][dCol];
		
		
		for(int row=0;row<dRow;row++){
			if(row==0){
				for(int col=0;col<dCol;col++){
					saECGData[row][col] = saLeadName[col];
				}
			}else{
				for(int col=0;col<dCol;col++){
					saECGData[row][col] = Double.toString(ecgData[row-1][col]);// row-1 because ecgData does not have header row.
				}
			}
		}
		return saECGData;
	}
	
	/** Returns the ECG data segment as a single string, ready to be transfered directly to JavaScript for use by Dygraphs.<BR>
	 * It includes the sample time-stamp in milliseconds, commas between column data and CR-LF at the end of each row.
	 * @return - data String ready for Dygraph 
	 */
	public String getECGDataSingleString() {
		int dRow = ecgData.length+1;
		int dCol = ecgData[0].length; // ecgDataLeads

		StringBuilder sECGData = new StringBuilder();
		StringBuilder sRow;
		
		for(int row=0;row<dRow;row++){
			sRow =  new StringBuilder();
			if(row==0){
				for(int col=0;col<dCol;col++){
					sRow.append(saLeadName[col]).append(',');
				}
			}else{
				for(int col=0;col<dCol;col++){
					sRow.append(Double.toString(ecgData[row-1][col])).append(','); // row-1 because ecgData does not have header row.
				}
			}
			sRow.deleteCharAt(sRow.length()-1);
			sRow.append('\n');
			sECGData.append(sRow);
		}

		return sECGData.toString();
	}

}
