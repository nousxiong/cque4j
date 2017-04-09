/**
 * 
 */
package cque.util;

/**
 * @author Xiong
 *
 */
public class RuntimeInterruptedException extends RuntimeException {
	private static final long serialVersionUID = -3765298180471880498L;

	public RuntimeInterruptedException(InterruptedException ie){
		super(ie);
	}
}
