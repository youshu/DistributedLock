package com.youshusoft;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import redis.clients.jedis.Jedis;

public class RedisLock implements Lock {
	 	private volatile Thread currentThread; //��������߳�
	    private final ConcurrentLinkedQueue<Thread> queue = new ConcurrentLinkedQueue<Thread>(); //���صȴ��̶߳���
	    private final AtomicInteger atomic = new AtomicInteger(); //cas��
	    public final Jedis jedis; //redis�ͻ���,���̰߳�ȫ��
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
	    	// �Ⱦ���������,�پ���redis��,����redis�����ͱ�֤Jedis�̰߳�ȫ
	    	for(;;){
	    		if(cas(0, 1)){
	    			//��ñ�����
	    			System.out.println("cas lock acquire succeed��" + thread);
	            	this.currentThread = thread;
	            	for(;;){
	            		if(setnx()){
	    	        		//�����
	    		        	System.out.println("redis lock acquire succeed��" + thread);
	    		        	break;
	    	        	}
	            		sleep(100);
	            	}
		        	break;
		        }else{
	        		//δ�����,�������
		        	queue.add(thread);
		        	LockSupport.park(this);//�ȴ��ź�
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
    		
    		System.out.println("lock release succeed��" + Thread.currentThread());
            Thread thread = queue.poll();
            if(thread != null){
                LockSupport.unpark(thread); //���ͼ����ź�
                System.out.println("unpark");
            }
            System.out.println("unlock end");

	    }
	    /**
	     * ִ�� redis setnx ��ȡ��
	     * @return
	     */
	    public boolean setnx(){
	    	System.out.println("setnx " + Thread.currentThread());
	        boolean result = false;
	        try{
    			result = jedis.setnx(lockKey,"1") == 1;
			} catch (Exception e) {
				e.printStackTrace();
				//�쳣,�ȴ�60s
				sleep(60000);
			}
	        return result;
	    }
	    /**
	     * ִ�� redis del �ͷ���
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
					//�쳣60s������
					sleep(60000);
				}
        	}
	        cas(1, 0); //�ͷű�����
	        return result;
	    }
	    @Override
	    public void lockInterruptibly() throws InterruptedException {
	    	throw new UnsupportedOperationException();

	    }

	    @Override
	    public boolean tryLock() {
	    	throw new UnsupportedOperationException();
	    }

	    @Override
	    public boolean tryLock(long time, TimeUnit unit)
	            throws InterruptedException {
	    	throw new UnsupportedOperationException();
	    }


	    @Override
	    public Condition newCondition() {
	    	throw new UnsupportedOperationException();
	    }
	    private boolean cas(int expect,int update){
	    	System.out.println("cas "+ expect +","+ update +" " + Thread.currentThread());
	    	return atomic.compareAndSet(expect, update);
	    }
	    
	    private void sleep(long waitTime){
	    	try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
	    }

}
