/**
 * 
 */
package cque;


/**
 * @author Xiong
 *
 */
public final class NodeStack {
	/**
	 * 压入堆栈新元素
	 * @param stack 堆栈
	 * @param t 新元素
	 * @return 新stack
	 */
	public static <T extends AbstractNode> T push(T stack, T t){
		if (stack != null){
			t.next = stack;
		}
		
		stack = t;
		return stack;
	}
	
	/**
	 * 从堆栈中弹出元素
	 * @param stack
	 * @return 新stack
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AbstractNode> T pop(T stack){
		if (stack == null){
			return null;
		}
		
		AbstractNode nx = stack.next;
		stack.next = null;
		return (T) nx;
	}
}
