package cn.iocoder.yudao.module.bpm.framework.flowable.config;

import cn.hutool.core.collection.ListUtil;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.TimeoutBpmnParseHandler;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

/**
 * BPM 模块的 Flowable 配置类
 *
 * @author jason
 */
@Configuration(proxyBeanMethods = false)
public class BpmFlowableConfiguration {

//    @PostConstruct
//    public void registerLiquibase() {
//        try {
//            // 注册自定义的 Kingbase 实现，让 Liquibase 遇到 KingbaseES 时也能处理
//            DatabaseFactory.getInstance().register(new KingbaseESDatabase());
//            System.out.println("=== Flowable Liquibase Adapter for KingbaseES Registered Successfully ===");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    static {
//        try {
//            DatabaseFactory.getInstance().register(new KingbaseESDatabase());
//            System.out.println("=== [Static Block] Flowable Liquibase Adapter for KingbaseES Registered ===");
//        } catch (Exception e) {
//            System.err.println("Failed to register KingbaseES adapter: " + e.getMessage());
//        }
//    }

    /**
     * 参考 {@link org.flowable.spring.boot.FlowableJobConfiguration} 类，创建对应的 AsyncListenableTaskExecutor Bean
     *
     * 如果不创建，会导致项目启动时，Flowable 报错的问题
     */
    @Bean(name = "applicationTaskExecutor")
    @ConditionalOnMissingBean(name = "applicationTaskExecutor")
    public AsyncListenableTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("flowable-task-Executor-");
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    /**
     * BPM 模块的 ProcessEngineConfigurationConfigurer 实现类：
     *
     * 1. 设置各种监听器
     * 2. 设置自定义的 ActivityBehaviorFactory 实现
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> bpmProcessEngineConfigurationConfigurer(
            ObjectProvider<FlowableEventListener> listeners,
            ObjectProvider<FlowableFunctionDelegate> customFlowableFunctionDelegates,
            BpmActivityBehaviorFactory bpmActivityBehaviorFactory,
            TimeoutBpmnParseHandler timeoutBpmnParseHandler) {
        return configuration -> {
            configuration.setDatabaseType("postgres");
            // 注册监听器，例如说 BpmActivityEventListener
            configuration.setEventListeners(ListUtil.toList(listeners.iterator()));
            // 设置 ActivityBehaviorFactory 实现类，用于流程任务的审核人的自定义
            configuration.setActivityBehaviorFactory(bpmActivityBehaviorFactory);
            // 设置自定义的函数
            configuration.setCustomFlowableFunctionDelegates(ListUtil.toList(customFlowableFunctionDelegates.stream().iterator()));
            configuration.setPostBpmnParseHandlers(ListUtil.toList(timeoutBpmnParseHandler));
        };
    }

    // =========== 审批人相关的 Bean ==========

    @Bean
    public BpmActivityBehaviorFactory bpmActivityBehaviorFactory(BpmTaskCandidateInvoker bpmTaskCandidateInvoker) {
        BpmActivityBehaviorFactory bpmActivityBehaviorFactory = new BpmActivityBehaviorFactory();
        bpmActivityBehaviorFactory.setTaskCandidateInvoker(bpmTaskCandidateInvoker);
        return bpmActivityBehaviorFactory;
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // adminUserApi 可以注入成功
    public BpmTaskCandidateInvoker bpmTaskCandidateInvoker(List<BpmTaskCandidateStrategy> strategyList,
                                                           AdminUserApi adminUserApi) {
        return new BpmTaskCandidateInvoker(strategyList, adminUserApi);
    }

    // =========== 自己拓展的 Bean ==========

    @Bean
    public BpmProcessInstanceEventPublisher processInstanceEventPublisher(ApplicationEventPublisher publisher) {
        return new BpmProcessInstanceEventPublisher(publisher);
    }

    public static class KingbaseESDatabase extends PostgresDatabase {

        // 提高优先级，确保 Liquibase 优先使用这个类来处理 Kingbase 连接
        @Override
        public int getPriority() {
            return PRIORITY_DATABASE + 5;
        }

        // 核心判断：当数据库产品名为 KingbaseES 时，返回 true
        @Override
        public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException, DatabaseException {
            return "KingbaseES".equalsIgnoreCase(conn.getDatabaseProductName());
        }

        @Override
        protected String getDefaultDatabaseProductName() {
            return "KingbaseES";
        }

        // 注意：不要覆盖 getShortName()，让它默认返回 "postgresql"
        // 这样 Liquibase 就会去加载 flowable-db-changelog-postgresql.xml
    }

}