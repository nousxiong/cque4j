/**
 * 
 */
package cque;

import java.lang.reflect.Field;

/**
 * @author Xiong
 * Unsafe的获取封装
 * 如果需要，请忽略ide，例如eclipse：
 * Preferences->Java->Compiler->Errors/Warnings->Deprecated and restricted API->Forbidden reference->Warning
 */
@SuppressWarnings({ "restriction" })
public class Unsafe {
	public static final sun.misc.Unsafe get() throws Exception{
		// 用反射绕过Unsafe的验证代码
		Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		return (sun.misc.Unsafe) f.get(null);
	}
}
