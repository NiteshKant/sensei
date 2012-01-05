package com.sensei.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.DefaultFacetHandlerInitializerParam;
import com.sensei.search.nodes.SenseiBroker;
import com.sensei.search.req.SenseiHit;
import com.sensei.search.req.SenseiRequest;
import com.sensei.search.req.SenseiResult;
import com.sensei.search.svc.api.SenseiService;

public class TestSensei extends TestCase {

  private static final Logger logger = Logger.getLogger(TestSensei.class);

  private static SenseiBroker broker;
  private static SenseiService httpRestSenseiService;
  static {
    SenseiStarter.start();
    broker = SenseiStarter.broker;
    httpRestSenseiService = SenseiStarter.httpRestSenseiService;

  }

  public void testTotalCount() throws Exception
  {
    logger.info("executing test case testTotalCount");
    SenseiRequest req = new SenseiRequest();
    SenseiResult res = broker.browse(req);
    assertEquals("wrong total number of hits" + req + res, 15000, res.getNumHits());
    logger.info("request:" + req + "\nresult:" + res);
  }

  public void testTotalCountWithFacetSpec() throws Exception
  {
    logger.info("executing test case testTotalCountWithFacetSpec");
    SenseiRequest req = new SenseiRequest();
    FacetSpec facetSpecall = new FacetSpec();
    facetSpecall.setMaxCount(1000000);
    facetSpecall.setExpandSelection(true);
    facetSpecall.setMinHitCount(0);
    facetSpecall.setOrderBy(FacetSortSpec.OrderHitsDesc);
    FacetSpec facetSpec = new FacetSpec();
    facetSpec.setMaxCount(5);
    setspec(req, facetSpec);
    req.setCount(5);
    setspec(req, facetSpecall);
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    verifyFacetCount(res, "year", "[1993 TO 1994]", 3090);
  }

  public void testSelection() throws Exception
  {
    logger.info("executing test case testSelection");
    FacetSpec facetSpecall = new FacetSpec();
    facetSpecall.setMaxCount(1000000);
    facetSpecall.setExpandSelection(true);
    facetSpecall.setMinHitCount(0);
    facetSpecall.setOrderBy(FacetSortSpec.OrderHitsDesc);
    FacetSpec facetSpec = new FacetSpec();
    facetSpec.setMaxCount(5);
    SenseiRequest req = new SenseiRequest();
    req.setCount(3);
    facetSpecall.setMaxCount(3);
    setspec(req, facetSpecall);
    BrowseSelection sel = new BrowseSelection("year");
    String selVal = "[2001 TO 2002]";
    sel.addValue(selVal);
    req.addSelection(sel);
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    assertEquals(2907, res.getNumHits());
    String selName = "year";
    verifyFacetCount(res, selName, selVal, 2907);
    verifyFacetCount(res, "year", "[1993 TO 1994]", 3090);
  }
  public void testSelectionDynamicTimeRange() throws Exception
  {
    logger.info("executing test case testSelection");


    SenseiRequest req = new SenseiRequest();
    DefaultFacetHandlerInitializerParam initParam = new DefaultFacetHandlerInitializerParam();
    initParam.putLongParam("time", new long[]{15000L});
    req.setFacetHandlerInitializerParam("timeRange", initParam);
    //req.setFacetHandlerInitializerParam("timeRange_internal", new DefaultFacetHandlerInitializerParam());
    req.setCount(3);
    //setspec(req, facetSpecall);
    BrowseSelection sel = new BrowseSelection("timeRange");
    String selVal = "000000013";
    sel.addValue(selVal);
    req.addSelection(sel);
     SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    assertEquals(12990, res.getNumHits());

  }
  public void testSelectionNot() throws Exception
  {
    logger.info("executing test case testSelectionNot");
    FacetSpec facetSpecall = new FacetSpec();
    facetSpecall.setMaxCount(1000000);
    facetSpecall.setExpandSelection(true);
    facetSpecall.setMinHitCount(0);
    facetSpecall.setOrderBy(FacetSortSpec.OrderHitsDesc);
    FacetSpec facetSpec = new FacetSpec();
    facetSpec.setMaxCount(5);
    SenseiRequest req = new SenseiRequest();
    req.setCount(3);
    facetSpecall.setMaxCount(3);
    setspec(req, facetSpecall);
    BrowseSelection sel = new BrowseSelection("year");
    sel.addNotValue("[2001 TO 2002]");
    req.addSelection(sel);
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    assertEquals(12093, res.getNumHits());
    verifyFacetCount(res, "year", "[1993 TO 1994]", 3090);
  }

  public void testGroupBy() throws Exception
  {
    logger.info("executing test case testGroupBy");
    SenseiRequest req = new SenseiRequest();
    req.setCount(1);
    req.setGroupBy("groupid");
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    SenseiHit hit = res.getSenseiHits()[0];
    assertTrue(hit.getGroupHitsCount() > 0);
  }

  public void testGroupByWithGroupedHits() throws Exception
  {
    logger.info("executing test case testGroupBy");
    SenseiRequest req = new SenseiRequest();
    req.setCount(1);
    req.setGroupBy("groupid");
    req.setMaxPerGroup(8);
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    SenseiHit hit = res.getSenseiHits()[0];
    assertTrue(hit.getGroupHitsCount() > 0);
    assertTrue(hit.getSenseiGroupHits().length > 0);

    // use httpRestSenseiService
    res = httpRestSenseiService.doQuery(req);
    logger.info("request:" + req + "\nresult:" + res);
    hit = res.getSenseiHits()[0];
    assertTrue(hit.getGroupHitsCount() > 0);
    assertTrue(hit.getSenseiGroupHits().length > 0);
  }

  public void testGroupByVirtual() throws Exception
  {
    logger.info("executing test case testGroupByVirtual");
    SenseiRequest req = new SenseiRequest();
    req.setCount(1);
    req.setGroupBy("virtual_groupid");
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    SenseiHit hit = res.getSenseiHits()[0];
    assertTrue(hit.getGroupHitsCount() > 0);
  }

  public void testGroupByVirtualWithGroupedHits() throws Exception
  {
    logger.info("executing test case testGroupByVirtualWithGroupedHits");
    SenseiRequest req = new SenseiRequest();
    req.setCount(1);
    req.setGroupBy("virtual_groupid");
    req.setMaxPerGroup(8);
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    SenseiHit hit = res.getSenseiHits()[0];
    assertTrue(hit.getGroupHitsCount() > 0);
    assertTrue(hit.getSenseiGroupHits().length > 0);
  }

  public void testGroupByFixedLengthLongArray() throws Exception
  {
    logger.info("executing test case testGroupByFixedLengthLongArray");
    SenseiRequest req = new SenseiRequest();
    req.setCount(1);
    req.setGroupBy("virtual_groupid_fixedlengthlongarray");
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    SenseiHit hit = res.getSenseiHits()[0];
    assertTrue(hit.getGroupHitsCount() > 0);
  }

  public void testGroupByFixedLengthLongArrayWithGroupedHits() throws Exception
  {
    logger.info("executing test case testGroupByFixedLengthLongArrayWithGroupedHits");
    SenseiRequest req = new SenseiRequest();
    req.setCount(1);
    req.setGroupBy("virtual_groupid_fixedlengthlongarray");
    req.setMaxPerGroup(8);
    SenseiResult res = broker.browse(req);
    logger.info("request:" + req + "\nresult:" + res);
    SenseiHit hit = res.getSenseiHits()[0];
    assertTrue(hit.getGroupHitsCount() > 0);
    assertTrue(hit.getSenseiGroupHits().length > 0);
  }

  public void testSelectionTerm() throws Exception
  {
    logger.info("executing test case Selection term");
    String req = "{\"selections\":[{\"term\":{\"color\":{\"value\":\"red\"}}}]}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 2160, res.getInt("numhits"));
  }

  public void testSelectionTerms() throws Exception
  {
    logger.info("executing test case Selection terms");
    String req = "{\"selections\":[{\"terms\":{\"tags\":{\"values\":[\"mp3\",\"moon-roof\"],\"excludes\":[\"leather\"],\"operator\":\"or\"}}}]}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 4483, res.getInt("numhits"));
  }
  public void testSelectionDynamicTimeRangeJson() throws Exception
  {
    logger.info("executing test case Selection terms");
    String req = "{\"selections\":[{\"term\":{\"timeRange\":{\"value\":\"000000013\"}}}]" +
    		", \"facetInit\":{    \"timeRange\":{\"time\" :{  \"type\" : \"long\",\"values\" : [15000] }}}" +
    		"}";
    System.out.println(req);
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 12990, res.getInt("numhits"));
  }
  public void testSelectionRange() throws Exception
  {
    //2000 1548;
    //2001 1443;
    //2002 1464;
    // [2000 TO 2002]   ==> 4455
    // (2000 TO 2002)   ==> 1443
    // (2000 TO 2002]   ==> 2907
    // [2000 TO 2002)   ==> 2991
    {
      logger.info("executing test case Selection range [2000 TO 2002]");
      String req = "{\"selections\":[{\"range\":{\"year\":{\"to\":\"2002\",\"include_lower\":true,\"include_upper\":true,\"from\":\"2000\"}}}]}";
      JSONObject res = search(new JSONObject(req));
      assertEquals("numhits is wrong", 4455, res.getInt("numhits"));
    }

    {
      logger.info("executing test case Selection range (2000 TO 2002)");
      String req = "{\"selections\":[{\"range\":{\"year\":{\"to\":\"2002\",\"include_lower\":false,\"include_upper\":false,\"from\":\"2000\"}}}]}";
      JSONObject res = search(new JSONObject(req));
      assertEquals("numhits is wrong", 1443, res.getInt("numhits"));
    }

    {
      logger.info("executing test case Selection range (2000 TO 2002]");
      String req = "{\"selections\":[{\"range\":{\"year\":{\"to\":\"2002\",\"include_lower\":false,\"include_upper\":true,\"from\":\"2000\"}}}]}";
      JSONObject res = search(new JSONObject(req));
      assertEquals("numhits is wrong", 2907, res.getInt("numhits"));
    }

    {
      logger.info("executing test case Selection range [2000 TO 2002)");
      String req = "{\"selections\":[{\"range\":{\"year\":{\"to\":\"2002\",\"include_lower\":true,\"include_upper\":false,\"from\":\"2000\"}}}]}";
      JSONObject res = search(new JSONObject(req));
      assertEquals("numhits is wrong", 2991, res.getInt("numhits"));
    }

  }

  public void testMatchAllWithBoostQuery() throws Exception
  {
    logger.info("executing test case MatchAllQuery");
    String req = "{\"query\": {\"match_all\": {\"boost\": \"1.2\"}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 15000, res.getInt("numhits"));
  }

  public void testQueryStringQuery() throws Exception
  {
    logger.info("executing test case testQueryStringQuery");
    String req = "{\"query\": {\"query_string\": {\"query\": \"red AND cool\"}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 1070, res.getInt("numhits"));
  }

  public void testMatchAllQuery() throws Exception
  {
    logger.info("executing test case testMatchAllQuery");
    String req = "{\"query\": {\"match_all\": {}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 15000, res.getInt("numhits"));
  }

  public void testUIDQuery() throws Exception
  {
    logger.info("executing test case testUIDQuery");
    String req = "{\"query\": {\"ids\": {\"values\": [\"1\", \"4\", \"3\", \"2\", \"6\"], \"excludes\": [\"2\"]}}}";
    JSONObject res = search(new JSONObject(req));

    assertEquals("numhits is wrong", 4, res.getInt("numhits"));
    Set<Integer> expectedIds = new HashSet(Arrays.asList(new Integer[]{1, 3, 4, 6}));
    for (int i = 0; i < res.getInt("numhits"); ++i)
    {
      int uid = res.getJSONArray("hits").getJSONObject(i).getInt("_uid");
      assertTrue("_UID " + uid + " is not expected.", expectedIds.contains(uid));
    }
  }

  public void testTextQuery() throws Exception
  {
    logger.info("executing test case testTextQuery");
    String req = "{\"query\": {\"text\": {\"contents\": { \"value\": \"red cool\", \"operator\": \"and\"}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 1070, res.getInt("numhits"));
  }

  public void testTermQuery() throws Exception
  {
    logger.info("executing test case testTermQuery");
    String req = "{\"query\":{\"term\":{\"color\":\"red\"}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 2160, res.getInt("numhits"));
  }

  public void testTermsQuery() throws Exception
  {
    logger.info("executing test case testTermQuery");
    String req = "{\"query\":{\"terms\":{\"tags\":{\"values\":[\"leather\",\"moon-roof\"],\"excludes\":[\"hybrid\"],\"minimum_match\":0,\"operator\":\"or\"}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 5777, res.getInt("numhits"));
  }


  public void testBooleanQuery() throws Exception
  {
    logger.info("executing test case testBooleanQuery");
    String req = "{\"query\":{\"bool\":{\"must_not\":{\"term\":{\"category\":\"compact\"}},\"must\":{\"term\":{\"color\":\"red\"}}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 1652, res.getInt("numhits"));
  }


  public void testDistMaxQuery() throws Exception
  {
    //color red ==> 2160
    //color blue ==> 1104
    logger.info("executing test case testDistMaxQuery");
    String req = "{\"query\":{\"dis_max\":{\"tie_breaker\":0.7,\"queries\":[{\"term\":{\"color\":\"red\"}},{\"term\":{\"color\":\"blue\"}}],\"boost\":1.2}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3264, res.getInt("numhits"));
  }

  public void testPathQuery() throws Exception
  {
    logger.info("executing test case testPathQuery");
    String req = "{\"query\":{\"path\":{\"makemodel\":\"asian/acura/3.2tl\"}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 126, res.getInt("numhits"));
  }

  public void testPrefixQuery() throws Exception
  {
    //color blue ==> 1104
    logger.info("executing test case testPrefixQuery");
    String req = "{\"query\":{\"prefix\":{\"color\":{\"value\":\"blu\",\"boost\":2}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 1104, res.getInt("numhits"));
  }


  public void testWildcardQuery() throws Exception
  {
    //color blue ==> 1104
    logger.info("executing test case testWildcardQuery");
    String req = "{\"query\":{\"wildcard\":{\"color\":{\"value\":\"bl*e\",\"boost\":2}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 1104, res.getInt("numhits"));
  }

  public void testRangeQuery() throws Exception
  {
    logger.info("executing test case testRangeQuery");
    String req = "{\"query\":{\"range\":{\"year\":{\"to\":2000,\"boost\":2,\"from\":1999,\"_noOptimize\":false}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3015, res.getInt("numhits"));
  }

  public void testRangeQuery2() throws Exception
  {
    logger.info("executing test case testRangeQuery2");
    String req = "{\"query\":{\"range\":{\"year\":{\"to\":\"2000\",\"boost\":2,\"from\":\"1999\",\"_noOptimize\":true,\"_type\":\"int\"}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3015, res.getInt("numhits"));
  }


  public void testFilteredQuery() throws Exception
  {
    logger.info("executing test case testFilteredQuery");
    String req ="{\"query\":{\"filtered\":{\"query\":{\"term\":{\"color\":\"red\"}},\"filter\":{\"range\":{\"year\":{\"to\":2000,\"from\":1999}}}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 447, res.getInt("numhits"));
  }


  public void testSpanTermQuery() throws Exception
  {
    logger.info("executing test case testSpanTermQuery");
    String req = "{\"query\":{\"span_term\":{\"color\":\"red\"}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 2160, res.getInt("numhits"));
  }


  public void testSpanOrQuery() throws Exception
  {
    logger.info("executing test case testSpanOrQuery");
    String req = "{\"query\":{\"span_or\":{\"clauses\":[{\"span_term\":{\"color\":\"red\"}},{\"span_term\":{\"color\":\"blue\"}}]}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3264, res.getInt("numhits"));
  }


  public void testSpanNotQuery() throws Exception
  {
    logger.info("executing test case testSpanNotQuery");
    String req = "{\"query\":{\"span_not\":{\"exclude\":{\"span_term\":{\"contents\":\"red\"}},\"include\":{\"span_term\":{\"contents\":\"compact\"}}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 4596, res.getInt("numhits"));
  }

  public void testSpanNearQuery1() throws Exception
  {
    logger.info("executing test case testSpanNearQuery1");
    String req = "{\"query\":{\"span_near\":{\"in_order\":false,\"collect_payloads\":false,\"slop\":12,\"clauses\":[{\"span_term\":{\"contents\":\"red\"}},{\"span_term\":{\"contents\":\"compact\"}},{\"span_term\":{\"contents\":\"hybrid\"}}]}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 274, res.getInt("numhits"));
  }

  public void testSpanNearQuery2() throws Exception
  {
    logger.info("executing test case testSpanNearQuery2");
    String req = "{\"query\":{\"span_near\":{\"in_order\":true,\"collect_payloads\":false,\"slop\":0,\"clauses\":[{\"span_term\":{\"contents\":\"red\"}},{\"span_term\":{\"contents\":\"compact\"}},{\"span_term\":{\"contents\":\"favorite\"}}]}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 63, res.getInt("numhits"));
  }

  public void testSpanFirstQuery() throws Exception
  {
    logger.info("executing test case testSpanFirstQuery");
    String req = "{\"query\":{\"span_first\":{\"match\":{\"span_term\":{\"color\":\"red\"}},\"end\":2}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 2160, res.getInt("numhits"));
  }


  public void testUIDFilter() throws Exception
  {
    logger.info("executing test case testUIDFilter");
    String req = "{\"filter\": {\"ids\": {\"values\": [\"1\", \"2\", \"3\"], \"excludes\": [\"2\"]}}}";
    JSONObject res = search(new JSONObject(req));

    assertEquals("numhits is wrong", 2, res.getInt("numhits"));
    Set<Integer> expectedIds = new HashSet(Arrays.asList(new Integer[]{1, 3}));
    for (int i = 0; i < res.getInt("numhits"); ++i)
    {
      int uid = res.getJSONArray("hits").getJSONObject(i).getInt("_uid");
      assertTrue("_UID " + uid + " is not expected.", expectedIds.contains(uid));
    }
  }

  public void testAndFilter() throws Exception
  {
    logger.info("executing test case testAndFilter");
    String req = "{\"filter\":{\"and\":[{\"term\":{\"tags\":\"mp3\",\"_noOptimize\":false}},{\"term\":{\"color\":\"red\",\"_noOptimize\":false}}]}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 439, res.getInt("numhits"));
  }

  public void testOrFilter() throws Exception
  {
    logger.info("executing test case testOrFilter");
    String req = "{\"filter\":{\"or\":[{\"term\":{\"color\":\"blue\",\"_noOptimize\":true}},{\"term\":{\"color\":\"red\",\"_noOptimize\":true}}]}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3264, res.getInt("numhits"));
  }

  public void testOrFilter2() throws Exception
  {
    logger.info("executing test case testOrFilter2");
    String req = "{\"filter\":{\"or\":[{\"term\":{\"color\":\"blue\",\"_noOptimize\":false}},{\"term\":{\"color\":\"red\",\"_noOptimize\":false}}]}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3264, res.getInt("numhits"));
  }

  public void testOrFilter3() throws Exception
  {
    logger.info("executing test case testOrFilter3");
    String req = "{\"filter\":{\"or\":[{\"term\":{\"color\":\"blue\",\"_noOptimize\":true}},{\"term\":{\"color\":\"red\",\"_noOptimize\":false}}]}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3264, res.getInt("numhits"));
  }


  public void testBooleanFilter() throws Exception
  {
    logger.info("executing test case testBooleanFilter");
    String req = "{\"filter\":{\"bool\":{\"must_not\":{\"term\":{\"category\":\"compact\"}},\"should\":[{\"term\":{\"color\":\"red\"}},{\"term\":{\"color\":\"green\"}}],\"must\":{\"term\":{\"color\":\"red\"}}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 1652, res.getInt("numhits"));
  }

  public void testQueryFilter() throws Exception
  {
    logger.info("executing test case testQueryFilter");
    String req = "{\"filter\": {\"query\":{\"range\":{\"year\":{\"to\":2000,\"boost\":2,\"from\":1999,\"_noOptimize\":false}}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3015, res.getInt("numhits"));
  }

  /* Need to fix the bug in bobo and kamikazi, for details see the following two test cases:*/

//  public void testAndFilter1() throws Exception
//  {
//    logger.info("executing test case testAndFilter1");
//    String req = "{\"filter\":{\"and\":[{\"term\":{\"color\":\"blue\",\"_noOptimize\":false}},{\"query\":{\"term\":{\"category\":\"compact\"}}}]}}";
//    JSONObject res = search(new JSONObject(req));
//    assertEquals("numhits is wrong", 504, res.getInt("numhits"));
//  }
//
//  public void testQueryFilter1() throws Exception
//  {
//    logger.info("executing test case testQueryFilter1");
//    String req = "{\"filter\": {\"query\":{\"term\":{\"category\":\"compact\"}}}}";
//    JSONObject res = search(new JSONObject(req));
//    assertEquals("numhits is wrong", 4169, res.getInt("numhits"));
//  }


  /*  another weird bug may exist somewhere in bobo or kamikazi.*/
  /*  In the following two test cases, when modifying the first one by changing "tags" to "tag", it is supposed that
   *  Only the first test case is not correct, but the second one also throw one NPE, which is weird.
   * */
//  public void testAndFilter2() throws Exception
//  {
//    logger.info("executing test case testAndFilter2");
//    String req = "{\"filter\":{\"and\":[{\"term\":{\"tags\":\"mp3\",\"_noOptimize\":false}},{\"query\":{\"term\":{\"color\":\"red\"}}}]}}";
//    JSONObject res = search(new JSONObject(req));
//    assertEquals("numhits is wrong", 439, res.getInt("numhits"));
//  }
//
//  public void testOrFilter4() throws Exception
//  {
//    //color:blue  ==> 1104
//    //color:red   ==> 2160
//    logger.info("executing test case testOrFilter4");
//    String req = "{\"filter\":{\"or\":[{\"term\":{\"color\":\"blue\",\"_noOptimize\":false}},{\"query\":{\"term\":{\"color\":\"red\"}}}]}}";
//    JSONObject res = search(new JSONObject(req));
//    assertEquals("numhits is wrong", 3264, res.getInt("numhits"));
//  }


  public void testTermFilter() throws Exception
  {
    logger.info("executing test case testTermFilter");
    String req = "{\"filter\":{\"term\":{\"color\":\"red\"}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 2160, res.getInt("numhits"));
  }

  public void testTermsFilter() throws Exception
  {
    logger.info("executing test case testTermsFilter");
    String req = "{\"filter\":{\"terms\":{\"tags\":{\"values\":[\"leather\",\"moon-roof\"],\"excludes\":[\"hybrid\"],\"minimum_match\":0,\"operator\":\"or\"}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 5777, res.getInt("numhits"));
  }

  public void testRangeFilter() throws Exception
  {
    logger.info("executing test case testRangeFilter");
    String req = "{\"filter\":{\"range\":{\"year\":{\"to\":2000,\"boost\":2,\"from\":1999,\"_noOptimize\":false}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3015, res.getInt("numhits"));
  }

  public void testRangeFilter2() throws Exception
  {
    logger.info("executing test case testRangeFilter2");
    String req = "{\"filter\":{\"range\":{\"year\":{\"to\":\"2000\",\"boost\":2,\"from\":\"1999\",\"_noOptimize\":true,\"_type\":\"int\"}}}}";
    JSONObject res = search(new JSONObject(req));
    assertEquals("numhits is wrong", 3015, res.getInt("numhits"));
  }
  public void testRangeFilter3() throws Exception
  {
    logger.info("executing test case testRangeFilter3");
    String req = "{\"fetchStored\":true,\"selections\":[{\"term\":{\"color\":{\"value\":\"red\"}}}],\"from\":0,\"filter\":{\"query\":{\"query_string\":{\"query\":\"cool AND moon-roof AND hybrid\"}}},\"size\":10}";
    JSONObject res = search(new JSONObject(req));
    //TODO Sensei returns undeterministic results for this query. Will create a Jira issue
    assertTrue("numhits is wrong", res.getInt("numhits") > 10);
  }
  public void testGetStoreRequest() throws Exception
  {
    logger.info("executing test case testGetStoreRequest");
    String req = "[1,2,3,5]";
    JSONObject res = searchGet(new JSONArray(req));
    //TODO Sensei returns undeterministic results for this query. Will create a Jira issue
    assertTrue("numhits is wrong", res.length() == 4);
    assertNotNull("", res.get(String.valueOf(1)));
  }

  private JSONObject search(JSONObject req) throws Exception  {
    return  search(SenseiStarter.SenseiUrl, req.toString());
  }
  private JSONObject searchGet(JSONArray req) throws Exception  {
    return  search(new URL(SenseiStarter.SenseiUrl.toString() + "/get"), req.toString());
  }
  private JSONObject search(URL url, String req) throws Exception {
    URLConnection conn = url.openConnection();
    conn.setDoOutput(true);
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
    String reqStr = req;
    System.out.println("req: " + reqStr);
    writer.write(reqStr, 0, reqStr.length());
    writer.flush();
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
    StringBuilder sb = new StringBuilder();
    String line = null;
    while((line = reader.readLine()) != null)
      sb.append(line);
    String res = sb.toString();
    System.out.println("res: " + res);
    return new JSONObject(res);
  }

  private void setspec(SenseiRequest req, FacetSpec spec) {
    req.setFacetSpec("color", spec);
    req.setFacetSpec("category", spec);
    req.setFacetSpec("city", spec);
    req.setFacetSpec("makemodel", spec);
    req.setFacetSpec("year", spec);
    req.setFacetSpec("price", spec);
    req.setFacetSpec("mileage", spec);
    req.setFacetSpec("tags", spec);
  }







//  public void testSortBy() throws Exception
//  {
//    logger.info("executing test case testSortBy");
//    String req = "{\"sort\":[{\"color\":\"desc\"},\"_score\"],\"from\":0,\"size\":15000}";
//    JSONObject res = search(new JSONObject(req));
//    JSONArray jhits = res.optJSONArray("hits");
//    ArrayList<String> arColors = new ArrayList<String>();
//    for(int i=0; i<jhits.length(); i++){
//      JSONObject jhit = jhits.getJSONObject(i);
//      JSONArray jcolor = jhit.optJSONArray("color");
//      if(jcolor != null){
//        String color = jcolor.optString(0);
//        if(color != null)
//          arColors.add(color);
//      }
//    }
//    checkColorOrder(arColors);
//    //    assertEquals("numhits is wrong", 15000, res.getInt("numhits"));
//  }

  private void checkColorOrder(ArrayList<String> arColors)
  {
    assertTrue("must have 15000 results, size is:" + arColors.size(), arColors.size() == 15000);
    for(int i=0; i< arColors.size()-1; i++){
      String first = arColors.get(i);
      String next = arColors.get(i+1);
      int comp = first.compareTo(next);
      assertTrue("should >=0 (first= "+ first+"  next= "+ next+")", comp>=0);
    }
  }


  public void testSortByDesc() throws Exception

  {
    logger.info("executing test case testSortByDesc");
    String req = "{\"selections\": [{\"range\": {\"mileage\": {\"from\": 16000, \"include_lower\": false}}}, {\"range\": {\"year\": {\"from\": 2002, \"include_lower\": true, \"include_upper\": true, \"to\": 2002}}}], \"sort\":[{\"color\":\"desc\"}, {\"category\":\"asc\"}],\"from\":0,\"size\":15000}";
    JSONObject res = search(new JSONObject(req));
    JSONArray jhits = res.optJSONArray("hits");
    ArrayList<String> arColors = new ArrayList<String>();
    ArrayList<String> arCategories = new ArrayList<String>();
    for(int i=0; i<jhits.length(); i++){
      JSONObject jhit = jhits.getJSONObject(i);
      JSONArray jcolor = jhit.optJSONArray("color");
      if(jcolor != null){
        String color = jcolor.optString(0);
        if(color != null)
          arColors.add(color);
      }
      JSONArray jcategory = jhit.optJSONArray("category");
      if (jcategory != null)
      {
        String category = jcategory.optString(0);
        if (category != null)
        {
          arCategories.add(category);
        }
      }
    }
    checkOrder(arColors, arCategories, true, false);
  }

  public void testSortByAsc() throws Exception
  {
    logger.info("executing test case testSortByAsc");
    String req = "{\"selections\": [{\"range\": {\"mileage\": {\"from\": 16000, \"include_lower\": false}}}, {\"range\": {\"year\": {\"from\": 2002, \"include_lower\": true, \"include_upper\": true, \"to\": 2002}}}], \"sort\":[{\"color\":\"asc\"}, {\"category\":\"desc\"}],\"from\":0,\"size\":15000}";
    JSONObject res = search(new JSONObject(req));
    JSONArray jhits = res.optJSONArray("hits");
    ArrayList<String> arColors = new ArrayList<String>();
    ArrayList<String> arCategories = new ArrayList<String>();
    for(int i=0; i<jhits.length(); i++){
      JSONObject jhit = jhits.getJSONObject(i);
      JSONArray jcolor = jhit.optJSONArray("color");
      if(jcolor != null){
        String color = jcolor.optString(0);
        if(color != null)
          arColors.add(color);
      }
      JSONArray jcategory = jhit.optJSONArray("category");
      if (jcategory != null)
      {
        String category = jcategory.optString(0);
        if (category != null)
        {
          arCategories.add(category);
        }
      }
    }
    checkOrder(arColors, arCategories, false, true);
  }

  private void checkOrder(ArrayList<String> arColors,
                          ArrayList<String> arCategories,
                          boolean colorDesc,
                          boolean categoryDesc)
  {
    assertEquals("Color array and category array must have same size",
                 arColors.size(), arCategories.size());
    assertTrue("must have 3680 results, size is:" + arColors.size(), arColors.size() == 368);
    for(int i=0; i< arColors.size()-1; i++){
      String first = arColors.get(i);
      String next = arColors.get(i+1);
      String firstCategory = arCategories.get(i);
      String nextCategory = arCategories.get(i+1);

      // System.out.println(">>> color = " + first + ", category = " + firstCategory);

      int comp = first.compareTo(next);
      if (colorDesc)
      {
        assertTrue("should >=0 (first= "+ first+"  next= "+ next+")", comp>=0);
      }
      else
      {
        assertTrue("should <=0 (first= "+ first+"  next= "+ next+")", comp<=0);
      }
      if (comp == 0)
      {
        int compCategory = firstCategory.compareTo(nextCategory);
        if (categoryDesc)
        {
          assertTrue("should >=0 (firstCategory= "+ firstCategory +
                     ", nextCategory= " + nextCategory +")", compCategory >= 0);
        }
        else
        {
          assertTrue("should <=0 (firstCategory= "+ firstCategory +
                     ", nextCategory= "+ nextCategory+")", compCategory <= 0);
        }
      }
    }
  }


  /**
   * @param res
   *          result
   * @param selName
   *          the field name of the facet
   * @param selVal
   *          the value for which to check the count
   * @param count
   *          the expected count of the given value. If count>0, we verify the count. If count=0, it either has to NOT exist or it is 0.
   *          If count <0, it must not exist.
   */
  private void verifyFacetCount(SenseiResult res, String selName, String selVal, int count)
  {
    FacetAccessible year = res.getFacetAccessor(selName);
    List<BrowseFacet> browsefacets = year.getFacets();
    int index = indexOfFacet(selVal, browsefacets);
    if (count>0)
    {
    assertTrue("should contain a BrowseFacet for " + selVal, index >= 0);
    BrowseFacet bf = browsefacets.get(index);
    assertEquals(selVal + " has wrong count ", count, bf.getFacetValueHitCount());
    } else if (count == 0)
    {
      if (index >= 0)
      {
        // count has to be 0
        BrowseFacet bf = browsefacets.get(index);
        assertEquals(selVal + " has wrong count ", count, bf.getFacetValueHitCount());
      }
    } else
    {
      assertTrue("should not contain a BrowseFacet for " + selVal, index < 0);
    }
  }

  private int indexOfFacet(String selVal, List<BrowseFacet> browsefacets)
  {
    for (int i = 0; i < browsefacets.size(); i++)
    {
      if (browsefacets.get(i).getValue().equals(selVal))
        return i;
    }
    return -1;
  }

}