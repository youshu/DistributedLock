package com.youshusoft;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import redis.clients.jedis.Jedis;

public class RedisLock implements Lock {
	 	private volatile Thread currentThread; //获得锁的线程
	    private final ConcurrentLinkedQueue<Thread> queue = new ConcurrentLinkedQueue<Thread>(); //本地等待线程队列
	    private final AtomicInteger atomic = new AtomicInteger(); //cas用
	    public final Jedis jedis; //redis客户端,非线程安全的
	    private final String lockKey;
	    
	    public RedisLock(String lockKey,String host) {
	    	jedis = new Jedis(host);
	        this.lockKey = lockKey;
	    }
	    public RedisLock(String lockKey,String host,int port) {
	    	jedis = new Jedis(host, port);
	    	this.lockKey = lockKey;
	    }

	    @Override
	    public void lock() {
	    	Thread thread = Thread.currentThread();
	    	// 先竞争本地锁,再竞争redis锁,减少redis操作和保证Jedis线程安全
	    	for(;;){
	    		if(cas(0, 1)){
	    			//获得本地锁
	    			System.out.println("cas lock acquire succeed！" + thread);
	            	this.currentThread = thread;
	            	for(;;){
	            		if(setnx()){
	    	        		//获得锁
	    		        	System.out.println("redis lock acquire succeed！" + thread);
	    		        	break;
	    	        	}
	            		sleep(100);
	            	}
		        	break;
		        }else{
	        		//未获得锁,加入队列
		        	queue.add(thread);
		        	LockSupport.park(this);//等待信号
		        	System.out.println("park " + thread);
		            
		        }
	    	}
	        
	    }
	    
	    @Override
	    public void unlock() {
	    	if(currentThread != Thread.currentThread()){
	    		return;
	    	}
	    	currentThread = null;
    		release();
    		
    		System.out.println("lock release succeed！" + Thread.currentThread());
            Thread thread = queue.poll();
            if(thread != null){
                LockSupport.unpark(thread); //发送激活信号
                System.out.println("unpark");
            }
            System.out.println("unlock end");

	    }
	    /**
	     * 执行 redis setnx 获取锁
	     * @return
	     */
	    public boolean setnx(){
	    	System.out.println("setnx " + Thread.currentThread());
	        boolean result = false;
	        try{
    			result = jedis.setnx(lockKey,"1") == 1;
			} catch (Exception e) {
				e.printStackTrace();
				//异常,等待60s
				sleep(60000);
			}
	        return result;
	    }
	    /**
	     * 执行 redis del 释放锁
	     * @return
	     */
	    public boolean release(){
	    	System.out.println("release " + Thread.currentThread());
	        boolean result = false;
			for(;;){
				try {
					result = jedis.del(lockKey) == 1;
        			break;
				} catch (Exception e) {
					e.printStackTrace();
					//异常60s后重试
					sleep(60000);
				}
        	}
	        cas(1, 0); //释放本地锁
	        return result;
	    }
	    @Override
	    public void lockInterruptibly() throws InterruptedException {
	        // TODO Auto-generated method stub

	    }

	    @Override
	    public boolean tryLock() {
	        // TODO Auto-generated method stub
	        return false;
	    }

	    @Override
	    public boolean tryLock(long time, TimeUnit unit)
	            throws InterruptedException {
	        // TODO Auto-generated method stub
	        return false;
	    }


	    @Override
	    public Condition newCondition() {
	        // TODO Auto-generated method stub
	        return null;
	    }
	    private boolean cas(int expect,int update){
	    	System.out.println("cas "+ expect +","+ update +" " + Thread.currentThread());
	    	return atomic.compareAndSet(expect, update);
	    }
	    
	    public void sleep(long waitTime){
	    	try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
	    }

}
