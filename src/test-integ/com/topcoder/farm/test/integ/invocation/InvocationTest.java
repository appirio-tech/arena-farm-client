/*
 * InvocationTest
 * 
 * Created 09/22/2006
 */
package com.topcoder.farm.test.integ.invocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import com.topcoder.farm.client.invoker.AsyncInvocationResponse;
import com.topcoder.farm.client.invoker.FarmFactory;
import com.topcoder.farm.client.invoker.FarmInvoker;
import com.topcoder.farm.client.invoker.InvocationResultHandler;
import com.topcoder.farm.controller.api.InvocationRequest;
import com.topcoder.farm.controller.api.InvocationRequestRef;
import com.topcoder.farm.controller.api.InvocationResponse;
import com.topcoder.farm.controller.exception.DuplicatedIdentifierException;
import com.topcoder.farm.shared.expression.Expressions;
import com.topcoder.farm.shared.invocation.InvocationRequirements;
import com.topcoder.farm.shared.util.concurrent.WaitFlag;
import com.topcoder.farm.test.common.TestDataPropertiesHelper;
import com.topcoder.farm.test.common.TestInvocation;
import com.topcoder.farm.test.integ.config.ClientTestIntegConfigurator;

/**
 * In order to run this test case, a controller must be running and configured 
 * to listen clients. Additionally only processor PR1-1 must be running and connected to the
 * farm.
 * 
 * Test case of Invocation scheduling features
 * 
 * @author Diego Belfer (mural)
 * @version $Id$
 */
public class InvocationTest extends TestCase {
    private FarmFactory factory;
    private FarmInvoker invoker;
    private List responses = Collections.synchronizedList(new ArrayList());
    private WaitFlag responseArrived = new WaitFlag();

    public InvocationTest() {
        
    }

    protected void setUp() throws Exception {
        super.setUp();
        try {
            ClientTestIntegConfigurator.configure();
            factory = FarmFactory.getInstance();
            FarmFactory.getInstance().configureHandler("CL1", new HandlerToList(responses, responseArrived));
            invoker = factory.getInvoker("CL1");
        } catch (Exception e) {
            throw (IllegalStateException) new IllegalStateException()
                    .initCause(e);
        }
        invoker.getClientNode().cancelPendingRequests();
        responses.clear();
    }
    
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        FarmFactory.releaseInstance();
    }

    public void testInvocationSync() throws Exception {
        InvocationRequest inv = buildInvocationRequest("I1;", "STRING_TEST", true);
        AsyncInvocationResponse aresponse = invoker.scheduleInvocationSync(inv);
        InvocationResponse response = aresponse.get(5000);
        TestInvocation result  = getTestInvocationResult(response);
        assertEquals("STRING_TEST", result.getText());
        assertEquals(0, responses.size());
    }
    
    public void testInvocationASync() throws Exception {
        InvocationRequest inv = buildInvocationRequest("I1;", "STRING_TEST", true);
        invoker.scheduleInvocation(inv);
        responseArrived.await(5000);
        assertEquals(1, responses.size());
        InvocationResponse response = (InvocationResponse) responses.get(0);
        TestInvocation result  = getTestInvocationResult(response);
        assertEquals("STRING_TEST", result.getText());
    }
    
    public void testDupInvocationException() throws Exception {
        InvocationRequest inv = buildInvocationRequest("I1;", "STRING_TEST", false);
        invoker.scheduleInvocation(inv);
        inv = buildInvocationRequest("I1;", "STRING_TEST1", false);
        try {
            invoker.scheduleInvocation(inv);
            fail("Expected Exception");
        } catch (DuplicatedIdentifierException e) {
            //Expected
        }
    }
    
    public void testCountInvocation() throws Exception {
        invoker.scheduleInvocation(buildInvocationRequest("I-1-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-1-2;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-2-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-2-2;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("F-1-1;", "STRING_TEST", false));
        Integer total = invoker.getClientNode().countPendingRequests();
        assertEquals(5, total.intValue());
        Integer i11 = invoker.getClientNode().countPendingRequests("I-1-1");
        assertEquals(1, i11.intValue());
        Integer i1 = invoker.getClientNode().countPendingRequests("I-1-");
        assertEquals(2, i1.intValue());
        Integer i2 = invoker.getClientNode().countPendingRequests("I-2-");
        assertEquals(2, i2.intValue());
        Integer i = invoker.getClientNode().countPendingRequests("I-");
        assertEquals(4, i.intValue());
        AsyncInvocationResponse response = invoker.scheduleInvocationSync(buildInvocationRequest("F-1-2;", "STRING_TEST", true));
        response.get(5000);
        Thread.sleep(100);
        Integer f = invoker.getClientNode().countPendingRequests("F-");
        assertEquals(1, f.intValue());
    }
    
    
    public void testCancelAllInvocations() throws Exception {
        invoker.scheduleInvocation(buildInvocationRequest("I-1-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-1-2;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-2-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-2-2;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("F-1-1;", "STRING_TEST", false));
        invoker.getClientNode().cancelPendingRequests();
        Integer all = invoker.getClientNode().countPendingRequests();
        assertEquals(0, all.intValue());
    }

    public void testCancelPrefixedInvocation() throws Exception {
        invoker.scheduleInvocation(buildInvocationRequest("I-1-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-1-2;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-2-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-2-2;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("F-1-1;", "STRING_TEST", false));
        invoker.getClientNode().cancelPendingRequests("I-1-1");
        Integer all = invoker.getClientNode().countPendingRequests();
        assertEquals(4, all.intValue());
        invoker.getClientNode().cancelPendingRequests("I-2-");
        all = invoker.getClientNode().countPendingRequests();
        assertEquals(2, all.intValue());
    }
    
    public void testOnceNotifiedIdCanBeReused() throws Exception {
        AsyncInvocationResponse aresponse = invoker.scheduleInvocationSync(buildInvocationRequest("I-1-1;", "STRING_TEST", true));
        aresponse.get(2000);
        Thread.sleep(100);
        AsyncInvocationResponse aresponse2 = invoker.scheduleInvocationSync(buildInvocationRequest("I-1-1;", "STRING_TEST", true));
        aresponse2.get(2000);
    }
    
    public void testEnqueuedItems() throws Exception {
        invoker.scheduleInvocation(buildInvocationRequest("F-1-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-1-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-1-2;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-2-1;", "STRING_TEST", false));
        invoker.scheduleInvocation(buildInvocationRequest("I-2-2;", "STRING_TEST", false));
        invoker.getClientNode().cancelPendingRequests("I-1-2");
        List enqueuedRequests = invoker.getClientNode().getEnqueuedRequests("I");
        assertEquals(3, enqueuedRequests.size());
        assertRef("I-1-1;", 0, enqueuedRequests);
        assertRef("I-2-1;", 1, enqueuedRequests);
        assertRef("I-2-2;", 2, enqueuedRequests);
        List allEnqueuedRequests = invoker.getClientNode().getEnqueuedRequests("");
        assertRef("F-1-1;", 0, allEnqueuedRequests);
        assertRef("I-1-1;", 1, allEnqueuedRequests);
        assertRef("I-2-1;", 2, allEnqueuedRequests);
        assertRef("I-2-2;", 3, allEnqueuedRequests);
    }


    public void testPriorityItems() throws Exception {
        FarmFactory.getInstance().configureHandler("CL10", new HandlerToList(responses, responseArrived));
        FarmInvoker lowPrioriyInvoker = FarmFactory.getInstance().getInvoker("CL10");
        //This blocks processor for a while
        lowPrioriyInvoker.scheduleInvocation(buildWaitRequest("CL10", "A-1-1;", "STRING_TEST", 1000));
        Thread.sleep(100);
        //These must be last
        lowPrioriyInvoker.scheduleInvocation(buildInvocationRequest("A-1-2;", "STRING_TEST", true));
        lowPrioriyInvoker.scheduleInvocation(buildInvocationRequest("A-2-1;", "STRING_TEST", true));
        AsyncInvocationResponse resp = lowPrioriyInvoker.scheduleInvocationSync(buildInvocationRequest("I-2-2;", "STRING_TEST", true));
        //these must be after the first 
        invoker.scheduleInvocation(buildInvocationRequest("B-1-1;", "STRING_TEST", true));
        invoker.scheduleInvocation(buildInvocationRequest("B-1-2;", "STRING_TEST", true));
        invoker.scheduleInvocation(buildInvocationRequest("B-2-1;", "STRING_TEST", true));
        invoker.scheduleInvocation(buildInvocationRequest("B-2-2;", "STRING_TEST", true));
        invoker.scheduleInvocation(buildWaitRequest("CL10", "I-1-1;", "STRING_TEST", 1000));
        
        resp.get(8000);
        
        assertEquals(8, responses.size());
        assertResponse("A-1-1;", 0, responses);
        assertResponse("B-1-1;", 1, responses);
        assertResponse("B-1-2;", 2, responses);
        assertResponse("B-2-1;", 3, responses);
        assertResponse("B-2-2;", 4, responses);
        assertResponse("I-1-1;", 5, responses);
        assertResponse("A-1-2;", 6, responses);
        assertResponse("A-2-1;", 7, responses);
    }

    public void testPriorityItemsAndCancellation() throws Exception {
        FarmFactory.getInstance().configureHandler("CL10", new HandlerToList(responses, responseArrived));
        FarmInvoker lowPrioriyInvoker = FarmFactory.getInstance().getInvoker("CL10");
        //This blocks processor for a while
        lowPrioriyInvoker.scheduleInvocation(buildWaitRequest("CL10", "A-1-1;", "STRING_TEST", 1000));
        Thread.sleep(100);
        //These must be last
        lowPrioriyInvoker.scheduleInvocation(buildInvocationRequest("A-1-2;", "STRING_TEST", true));
        lowPrioriyInvoker.scheduleInvocation(buildInvocationRequest("A-2-1;", "STRING_TEST", true));
        AsyncInvocationResponse resp = lowPrioriyInvoker.scheduleInvocationSync(buildInvocationRequest("I-2-2;", "STRING_TEST", true));
        //these must be after the first 
        invoker.scheduleInvocation(buildInvocationRequest("B-1-1;", "STRING_TEST", true));
        invoker.scheduleInvocation(buildInvocationRequest("B-1-2;", "STRING_TEST", true));
        invoker.scheduleInvocation(buildInvocationRequest("B-2-1;", "STRING_TEST", true));
        invoker.scheduleInvocation(buildInvocationRequest("B-2-2;", "STRING_TEST", true));
        invoker.scheduleInvocation(buildWaitRequest("CL10", "I-1-1;", "STRING_TEST", 1000));
        invoker.getClientNode().cancelPendingRequests("B-2-");
        resp.get(8000);
        
        assertEquals(6, responses.size());
        assertResponse("A-1-1;", 0, responses);
        assertResponse("B-1-1;", 1, responses);
        assertResponse("B-1-2;", 2, responses);
        assertResponse("I-1-1;", 3, responses);
        assertResponse("A-1-2;", 4, responses);
        assertResponse("A-2-1;", 5, responses);
    }
    private void assertResponse(String id, int i, List enqueuedRequests) {
        InvocationResponse response = ((InvocationResponse)enqueuedRequests.get(i));
        assertEquals(id, response.getRequestId());
        assertEquals(id+id, response.getAttachment());
    }
    
    private void assertRef(String id, int i, List enqueuedRequests) {
        InvocationRequestRef reqRef = ((InvocationRequestRef)enqueuedRequests.get(i));
        assertEquals(id, reqRef.getRequestId());
        assertEquals(id+id, reqRef.getAttachment());
    }

    InvocationRequest buildInvocationRequest(String requestId, String text, boolean accept) {
        InvocationRequest request = new InvocationRequest();
        request.setId(requestId);
        request.setRequirements(new InvocationRequirements());
        if (!accept) {
            request.getRequirements().setFilterExpression(Expressions.eq(TestDataPropertiesHelper.PROC_PROP_ID, new Integer(4)));
            
        }
        request.setAttachment(requestId+requestId);
        request.setInvocation(new TestInvocation(requestId, "CL1", text, 0, null));
        return request;
    }
    
    
    InvocationRequest buildWaitRequest(String clientId, String requestId, String text, int time) {
        InvocationRequest request = new InvocationRequest();
        request.setId(requestId);
        request.setRequirements(new InvocationRequirements());
        request.setAttachment(requestId+requestId);
        request.setInvocation(new TestInvocation(requestId, clientId, text, time, null));
        return request;
    }

    private TestInvocation getTestInvocationResult(InvocationResponse response) {
        assertFalse(response.getResult().isExceptionThrown());
        return (TestInvocation) response.getResult().getReturnValue();
    }
    
    private static final class HandlerToList implements InvocationResultHandler {
        private List responses;
        private WaitFlag responseArrived;
        
        public HandlerToList(List responses, WaitFlag responseArrived) {
            this.responses = responses;
            this.responseArrived = responseArrived;
        }

        public boolean handleResult(InvocationResponse response) {
            responses.add(response);
            responseArrived.set();
            return true;
        }
    }

}
