import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.youshusoft.RedisLock;

import redis.clients.jedis.Jedis;


public class Test{
	private static String host = "localhost";
	private static String lockKey = "redisLock";
	private static String testKey = "testData";
	private static RedisLock lock1 = new RedisLock(lockKey, host);
	private static RedisLock lock2 = new RedisLock(lockKey, host);
	public static void main(String[] args) throws Exception {
//		new Test().del(testKey);
//		new Test().get(testKey); //查看测试数据
//		new Test().del(lockKey);
//		new Test().get(lockKey); //查看锁状态
		new Test().test1();
		new Test().test2();
//		System.out.println(new RedisLock(lockKey, host).jedis.setnx(lockKey, "1"));
	}
	public void test1() throws Exception{
//		Thread.sleep(10000);
		
		Jedis jedis = new Jedis(host);
		List<Thread> list = new ArrayList<Thread>();
		for (int i = 0; i < 100; i++) {
			Thread thread = new Thread(new Runnable() {
//				RedisLock redisLock1 = new RedisLock(lockKey, host);
				@Override
				public void run() {
					for (int j = 0; j < 1000; j++) {
						lock1.lock();
						String s = jedis.get(testKey);
						int val = 0;
						if(s != null){
							val = Integer.valueOf(s);
						}
						val++;
						jedis.set(testKey, val + "");
						lock1.unlock();
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
				}
			});
			thread.start();
			list.add(thread);
		}
		for (Thread thread : list) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("end 1");
	}
	public void test2() throws Exception{
//		Thread.sleep(5000);
		Jedis jedis = new Jedis(host);
		
		List<Thread> list = new ArrayList<Thread>();
		for (int i = 0; i < 1000; i++) {
			Thread thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (int j = 0; j < 100; j++) {
						lock2.lock();
						String s = jedis.get(testKey);
						int val = 0;
						if(s != null){
							val = Integer.valueOf(s);
						}
						val++;
						jedis.set(testKey, val + "");
//						jedis.close();
						lock2.unlock();
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
				}
			});
			thread.start();
			list.add(thread);
		}
		for (Thread thread : list) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("end 2");
	}
	
	
	public void get(String key)  throws Exception{
		Jedis jedis = new Jedis(host);
		jedis.ping();
		System.out.println(jedis.get(key));
		jedis.close();
		System.out.println("end");
	}
	public void del(String key)  throws Exception{
		Jedis jedis = new Jedis(host);
		System.out.println(jedis.del(key));
		jedis.close();
	}

}
