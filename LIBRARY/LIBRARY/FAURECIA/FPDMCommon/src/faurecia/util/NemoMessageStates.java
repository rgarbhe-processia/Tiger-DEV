package faurecia.util;

/**
 * Enumeration for the states of messages sent by Enovia for Nemo Conversions
 */
public enum NemoMessageStates {
	/**
	 * if the job is enqueue
	 */
	OK,
	
	/**
	 * if the task is already in treatment queue
	 */
	ALREADY_IN_QUEUE,
	
	/**
	 * if the object is already converted with this options
	 */
	ALREADY_IN_VIEWABLES,
	
	/**
	 * Unknown state
	 */
	UNKNOWN,
	
	/**
	 * comboObject not recognized
	 */
	COMBOOBJECT_NOT_RECOGNIZED,
	
	/**
	 * Could not convert files due to technical error
	 */
	TECHNICAL_ERROR
}