package yaycrawler.worker.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import yaycrawler.common.model.*;
import yaycrawler.common.utils.HttpUtils;
import yaycrawler.worker.exception.WorkerHeartbeatFailureException;
import yaycrawler.worker.exception.WorkerRegisteFailureException;
import yaycrawler.worker.exception.WorkerResultNotifyFailureException;
import yaycrawler.worker.model.WorkerContext;

/**
 * Created by ucs_yuananyun on 2016/5/13.
 */
@Component
public class MasterActor {

    private static Logger logger = LoggerFactory.getLogger(MasterActor.class);

    @Value("${master.server.address}")
    private String masterServerAddress;

    @Value("${context.path}")
    private String contextPath;

    /**
     * Worker注册Master
     *
     * @return
     */
    public boolean register() {
        logger.info("worker-{}开始向Master申请注册", WorkerContext.workerId);
        WorkerRegistration workerRegistration = new WorkerRegistration(WorkerContext.workerId, contextPath);
        String targetUrl = CommunicationAPIs.getFullRemoteUrl(masterServerAddress, CommunicationAPIs.WORKER_POST_MASTER_REGISTER);
        RestFulResult result = HttpUtils.doHttpExecute(targetUrl, HttpMethod.POST, workerRegistration);
        if (result.hasError()) {
            logger.error("worker-{}注册Master失败！", WorkerContext.workerId);
            throw new WorkerRegisteFailureException(result.getMessage());
        }
        return true;
    }

    public boolean sendHeartbeart() {
        if (!WorkerContext.isSuccessRegisted) return false;

        WorkerHeartbeat heartbeat = new WorkerHeartbeat();
        heartbeat.setWorkerId(WorkerContext.workerId);
        heartbeat.setLastTime(System.currentTimeMillis());

        String targetUrl = CommunicationAPIs.getFullRemoteUrl(WorkerContext.masterServerAddress, CommunicationAPIs.WORKER_POST_MASTER_HEARTBEAT);
        RestFulResult result = HttpUtils.doHttpExecute(targetUrl, HttpMethod.POST, heartbeat);
        if (result.hasError())
            throw new WorkerHeartbeatFailureException();
        return true;
    }


    /**
     * Worker通知Master爬取结果成功
     *
     * @param crawlerResult
     * @return
     */
    public boolean notifyTaskSuccess(CrawlerResult crawlerResult) {
        logger.info("任务{}执行成功，现在通知Master……", crawlerResult.getKey());
        String targetUrl = CommunicationAPIs.getFullRemoteUrl(masterServerAddress, CommunicationAPIs.WORKER_POST_MASTER_SUCCESS_NOTIFY);
        RestFulResult result = HttpUtils.doHttpExecute(targetUrl, HttpMethod.POST, crawlerResult);
        if (result.hasError()) {
            logger.error("任务{}执行成功，通知Master失败！", crawlerResult.getKey());
            throw new WorkerResultNotifyFailureException(result.getMessage());
        }
        return true;
    }


    public boolean notifyTaskFailure(CrawlerResult crawlerResult) {
        logger.info("任务{}执行失败，现在通知Master……", crawlerResult.getKey());
        String targetUrl = CommunicationAPIs.getFullRemoteUrl(masterServerAddress, CommunicationAPIs.WORKER_POST_MASTER_FAILURE_NOTIFY);
        RestFulResult result = HttpUtils.doHttpExecute(targetUrl, HttpMethod.POST, crawlerResult);
        if (result.hasError()) {
            logger.error("任务{}执行失败，通知Master失败！", crawlerResult.getKey());
            throw new WorkerResultNotifyFailureException(result.getMessage());
        }
        return true;
    }
}