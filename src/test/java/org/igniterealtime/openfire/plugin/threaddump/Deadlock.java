/*
 * @(#)Deadlock.java  1.5 05/11/17
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */
package org.igniterealtime.openfire.plugin.threaddump;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Deadlock
{
    private CyclicBarrier barrier = new CyclicBarrier( 6 );

    public Deadlock()
    {
        DeadlockThread[] dThreads = new DeadlockThread[6];

        Monitor a = new Monitor( "a" );
        Monitor b = new Monitor( "b" );
        Monitor c = new Monitor( "c" );
        dThreads[0] = new DeadlockThread( "MThread-1", a, b );
        dThreads[1] = new DeadlockThread( "MThread-2", b, c );
        dThreads[2] = new DeadlockThread( "MThread-3", c, a );

        Lock d = new ReentrantLock();
        Lock e = new ReentrantLock();
        Lock f = new ReentrantLock();

        dThreads[3] = new DeadlockThread( "SThread-4", d, e );
        dThreads[4] = new DeadlockThread( "SThread-5", e, f );
        dThreads[5] = new DeadlockThread( "SThread-6", f, d );

        // make them daemon threads so that the test will exit
        for ( int i = 0; i < 6; i++ )
        {
            dThreads[i].setDaemon( true );
            dThreads[i].start();
        }
    }

    class DeadlockThread extends Thread
    {
        private Lock lock1 = null;

        private Lock lock2 = null;

        private Monitor mon1 = null;

        private Monitor mon2 = null;

        private boolean useSync;

        DeadlockThread( String name, Lock lock1, Lock lock2 )
        {
            super( name );
            this.lock1 = lock1;
            this.lock2 = lock2;
            this.useSync = true;
        }

        DeadlockThread( String name, Monitor mon1, Monitor mon2 )
        {
            super( name );
            this.mon1 = mon1;
            this.mon2 = mon2;
            this.useSync = false;
        }

        public void run()
        {
            if ( useSync )
            {
                syncLock();
            }
            else
            {
                monitorLock();
            }
        }

        private void syncLock()
        {
            lock1.lock();
            try
            {
                try
                {
                    barrier.await();
                }
                catch ( InterruptedException | BrokenBarrierException e )
                {
                    throw new IllegalStateException();
                }
                goSyncDeadlock();
            }
            finally
            {
                lock1.unlock();
            }
        }

        private void goSyncDeadlock()
        {
            try
            {
                barrier.await();
            }
            catch ( InterruptedException | BrokenBarrierException e )
            {
                throw new IllegalStateException();
            }
            lock2.lock();
            throw new RuntimeException( "should not reach here." );
        }

        private void monitorLock()
        {
            synchronized ( mon1 )
            {
                try
                {
                    barrier.await();
                }
                catch ( InterruptedException | BrokenBarrierException e )
                {
                    throw new IllegalStateException();
                }
                goMonitorDeadlock();
            }
        }

        private void goMonitorDeadlock()
        {
            try
            {
                barrier.await();
            }
            catch ( InterruptedException | BrokenBarrierException e )
            {
                throw new IllegalStateException();
            }
            synchronized ( mon2 )
            {
                throw new RuntimeException( getName() + " should not reach here." );
            }
        }
    }

    class Monitor
    {
        String name;

        Monitor( String name )
        {
            this.name = name;
        }
    }
}
