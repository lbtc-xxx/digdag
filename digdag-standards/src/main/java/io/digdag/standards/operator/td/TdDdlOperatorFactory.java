package io.digdag.standards.operator.td;

import java.util.List;
import java.nio.file.Path;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.common.collect.Iterables;
import io.digdag.client.config.ConfigException;
import io.digdag.spi.SecretNotFoundException;
import io.digdag.spi.TaskExecutionContext;
import io.digdag.spi.TaskRequest;
import io.digdag.spi.TaskResult;
import io.digdag.spi.Operator;
import io.digdag.spi.OperatorFactory;
import io.digdag.util.BaseOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.digdag.client.config.Config;

public class TdDdlOperatorFactory
        implements OperatorFactory
{
    private static Logger logger = LoggerFactory.getLogger(TdDdlOperatorFactory.class);

    @Inject
    public TdDdlOperatorFactory()
    { }

    public String getType()
    {
        return "td_ddl";
    }

    @Override
    public Operator newTaskExecutor(Path workspacePath, TaskRequest request)
    {
        return new TdDdlOperator(workspacePath, request);
    }

    private class TdDdlOperator
            extends BaseOperator
    {
        public TdDdlOperator(Path workspacePath, TaskRequest request)
        {
            super(workspacePath, request);
        }

        @Override
        public List<String> secretSelectors()
        {
            return ImmutableList.of("td.*");
        }

        @Override
        public TaskResult runTask(TaskExecutionContext ctx)
        {
            Config params = request.getConfig().mergeDefault(
                    request.getConfig().getNestedOrGetEmpty("td"));

            List<String> dropDatabaseList = params.getListOrEmpty("drop_databases", String.class);
            List<String> createDatabaseList = params.getListOrEmpty("create_databases", String.class);
            List<String> emptyDatabaseList = params.getListOrEmpty("empty_databases", String.class);

            try (TDOperator op = TDOperator.fromConfig(params, ctx.secrets().getSecrets("td"))) {
                for (String d : Iterables.concat(dropDatabaseList, emptyDatabaseList)) {
                    logger.info("Deleting TD database {}.{}", d);
                    op.withDatabase(d).ensureDatabaseDeleted(d);
                }
                for (String d : Iterables.concat(createDatabaseList, emptyDatabaseList)) {
                    logger.info("Creating TD database {}.{}", op.getDatabase(), d);
                    op.withDatabase(d).ensureDatabaseCreated(d);
                }
            }

            List<TableParam> dropTableList = params.getListOrEmpty("drop_tables", TableParam.class);
            List<TableParam> createTableList = params.getListOrEmpty("create_tables", TableParam.class);
            List<TableParam> emptyTableList = params.getListOrEmpty("empty_tables", TableParam.class);

            try (TDOperator op = TDOperator.fromConfig(params, ctx.secrets().getSecrets("td"))) {
                for (TableParam t : Iterables.concat(dropTableList, emptyTableList)) {
                    logger.info("Deleting TD table {}.{}", op.getDatabase(), t);
                    op.withDatabase(t.getDatabase().or(op.getDatabase())).ensureTableDeleted(t.getTable());
                }
                for (TableParam t : Iterables.concat(createTableList, emptyTableList)) {
                    logger.info("Creating TD table {}.{}", op.getDatabase(), t);
                    op.withDatabase(t.getDatabase().or(op.getDatabase())).ensureTableCreated(t.getTable());
                }
            }

            return TaskResult.empty(request);
        }
    }
}
