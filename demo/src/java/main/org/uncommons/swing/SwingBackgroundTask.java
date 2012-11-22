// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package org.uncommons.swing;

import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;

/**
 * A task that is executed on a background thread and then updates
 * a Swing GUI.  A task may only be executed once.
 * @author Daniel Dyer
 * @param <V> Type of result generated by the task.
 */
public abstract class SwingBackgroundTask<V>
{
    // Used to assign thread IDs to make threads easier to identify when debugging.
    private static int instanceCount = 0;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final int id;

    protected SwingBackgroundTask()
    {
        synchronized (SwingBackgroundTask.class)
        {
            this.id = instanceCount;
            ++instanceCount;
        }
    }


    /**
     * Asynchronous call that begins execution of the task
     * and returns immediately.
     */
    public void execute()
    {
        Runnable task = new Runnable()
        {
            public void run()
            {
                final V result = performTask();
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        postProcessing(result);
                        latch.countDown();
                    }
                });
            }
        };
        new Thread(task, "SwingBackgroundTask-" + id).start();
    }


    /**
     * Waits for the execution of this task to complete.  If the {@link #execute()}
     * method has not yet been invoked, this method will block indefinitely.
     * @throws InterruptedException If the thread executing the task
     * is interrupted.
     */
    public void waitForCompletion() throws InterruptedException
    {
        latch.await();
    }


    /**
     * Performs the processing of the task and returns a result.
     * Implement in sub-classes to provide the task logic.  This method will
     * run on a background thread and not on the Event Dispatch Thread and
     * therefore should not manipulate any Swing components.
     * @return The result of executing this task.
     */
    protected abstract V performTask();


    /**
     * This method is invoked, on the Event Dispatch Thread, after the task
     * has been executed.
     * This should be implemented in sub-classes in order to provide GUI
     * updates that should occur following task completion.
     * @param result The result from the {@link #performTask()} method.
     */
    protected abstract void postProcessing(V result);
}
