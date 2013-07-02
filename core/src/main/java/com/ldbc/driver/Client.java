package com.ldbc.driver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ldbc.driver.generator.GeneratorBuilder;
import com.ldbc.driver.measurements.MetricsExporterException;
import com.ldbc.driver.measurements.WorkloadMetricsManager;
import com.ldbc.driver.measurements.exporters.MetricsExporter;
import com.ldbc.driver.measurements.exporters.OutputStreamMetricsExporter;
import com.ldbc.driver.measurements.formatters.MetricsFormatter;
import com.ldbc.driver.measurements.formatters.SimpleMetricsFormatter;
import com.ldbc.driver.util.ClassLoaderHelper;
import com.ldbc.driver.util.MapUtils;
import com.ldbc.driver.util.Pair;
import com.ldbc.driver.util.RandomDataGeneratorFactory;
import com.ldbc.driver.util.temporal.Duration;
import com.ldbc.driver.util.temporal.Time;
import com.ldbc.driver.util.temporal.TimeUnit;

public class Client
{
    private static Logger logger = Logger.getLogger( Client.class );

    /*
     * For partitioning load among machines when client is bottleneck.
     *
     * INSERT_START
     * Specifies which record ID each client starts from - enables load phase to proceed from 
     * multiple clients on different machines.
     * 
     * INSERT_COUNT
     * Specifies number of inserts each client should do, if less than RECORD_COUNT.
     * Works in conjunction with INSERT_START, which specifies the record to start at (offset).
     *  
     * E.g. to load 1,000,000 records from 2 machines: 
     * client 1 --> insertStart=0
     *          --> insertCount=500,000
     * client 2 --> insertStart=50,000
     *          --> insertCount=500,000
    */
    public static final String INSERT_COUNT_ARG = "insertcount";
    public static final String INSERT_COUNT_DEFAULT = "0";
    public static final String INSERT_START_ARG = "insertstart";
    public static final String INSERT_START_DEFAULT = "-1";
    // TODO undocumented, document somehow
    public static final String RECORD_COUNT_ARG = "recordcount";
    public static final String RECORD_COUNT_DEFAULT = "-1";

    // --- COMPULSORY ---
    private static final String WORKLOAD_ARG = "workload";
    private static final String WORKLOAD_EXAMPLE = com.ldbc.driver.workloads.simple.SimpleWorkload.class.getName();
    private static final String DB_ARG = "db";
    private static final String DB_EXAMPLE = com.ldbc.driver.db.basic.BasicDb.class.getName();
    private static final String OPERATION_COUNT_ARG = "operationcount";
    private static final String OPERATION_COUNT_DEFAULT = "-1";
    // --- OPTIONAL ---
    private static final String THREADS_ARG = "threads";
    private static final String THREADS_DEFAULT = Integer.toString( defaultThreadCount() );
    private static final String BENCHMARK_PHASE_ARG = "phase";
    private static final String BENCHMARK_PHASE_DEFAULT = com.ldbc.driver.BenchmarkPhase.TRANSACTION_PHASE.toString();
    private static final String BENCHMARK_PHASE_LOAD = "load";
    private static final String BENCHMARK_PHASE_TRANSACTION = "transaction";
    private static final String SHOW_STATUS_ARG = "status";
    private static final String SHOW_STATUS_DEFAULT = "false";
    private static final String PROPERTY_FILE_ARG = "P";
    private static final String PROPERTY_ARG = "p";

    private static final String[] REQUIRED_PROPERTIES = new String[] { DB_ARG, WORKLOAD_ARG, OPERATION_COUNT_ARG };

    private static final long RANDOM_SEED = 42;

    public static void main( String[] args ) throws ClientException
    {
        Client client = new Client();
        try
        {
            Map<String, String> properties = parseArguments( args );

            Pair<Boolean, String> hasRequiredProperties = checkRequiredProperties( properties, REQUIRED_PROPERTIES );
            if ( false == hasRequiredProperties._1() )
            {
                String errMsg = hasRequiredProperties._2();
                logger.error( errMsg );
                logger.error( usageMessage() );
                throw new ClientException( errMsg );
            }

            logger.info( "LDBC Driver 0.1" );
            StringBuilder welcomeMessage = new StringBuilder();
            welcomeMessage.append( "Command line:" );
            for ( int i = 0; i < args.length; i++ )
            {
                welcomeMessage.append( " " + args[i] );
            }
            logger.info( welcomeMessage );

            client.start( properties );
        }
        catch ( ClientException e )
        {
            String errMsg = "Error while trying to parse properties";
            logger.error( errMsg, e );
        }
        catch ( Exception e )
        {
            logger.error( "Client terminated unexpectedly", e );
        }
        finally
        {
            client.exit();
        }
    }

    public static int defaultThreadCount()
    {
        // Client & OperationResultLoggingThread
        int threadsUsedByDriver = 2;
        int totalProcessors = Runtime.getRuntime().availableProcessors();
        int availableThreads = totalProcessors - threadsUsedByDriver;
        return Math.max( 1, availableThreads );
    }

    private static String usageMessage()
    {
        String usageMessage = String.format( "Usage: java %s [parameters]\n", Client.class.getName() )
                              + "\nRequired parameters:\n"
                              + String.format( "  -%s db_name: specify the name of the DB to use (e.g.: %s)\n", DB_ARG,
                                      DB_EXAMPLE )
                              + String.format( "  %s: name of the workload class to use (e.g. %s)\n", WORKLOAD_ARG,
                                      WORKLOAD_EXAMPLE )
                              + String.format(
                                      "  %s / %s: to run the transaction phase from multiple servers, start a separate client on each\n",
                                      INSERT_COUNT_ARG, INSERT_START_ARG )
                              + "  to run the load phase from multiple servers, start a separate client on each; additionally,\n"
                              + String.format(
                                      "  use the %s and %s properties to divide up the records to be inserted",
                                      INSERT_COUNT_ARG, INSERT_START_ARG )
                              + "\nOptional parameters:\n"
                              + String.format( "  -%s:  run the loading phase of the workload\n", BENCHMARK_PHASE_LOAD )
                              + String.format( "  -%s:  run the transactions phase of the workload (default)\n",
                                      BENCHMARK_PHASE_TRANSACTION )
                              + String.format( "  -%s number: execute using n threads (default: %s)\n", THREADS_ARG,
                                      defaultThreadCount() )
                              + String.format( "  -%s:  show status during run (default: %s)\n", SHOW_STATUS_ARG,
                                      SHOW_STATUS_DEFAULT )
                              + String.format(
                                      "  -%s property_file: load properties from the given file. Multiple files can\n",
                                      PROPERTY_FILE_ARG )
                              + "                   be specified, and will be processed in the order specified\n"
                              + String.format(
                                      "  -%s name=value:  specify a property to be passed to the DB and Workload;\n",
                                      PROPERTY_ARG )
                              + "                  multiple properties can be specified, and override any\n"
                              + "                  values in the property files\n";
        return usageMessage;
    }

    private static Map<String, String> parseArguments( String[] args ) throws ClientException
    {
        Map<String, String> commandlineProperties = new HashMap<String, String>();
        Map<String, String> fileProperties = new HashMap<String, String>();

        int argIndex = 0;

        if ( args.length == 0 )
        {
            String errMsg = usageMessage();
            logger.error( errMsg );
            throw new ClientException();
        }

        while ( args[argIndex].startsWith( "-" ) )
        {
            String arg = args[argIndex].substring( 1 );

            // Binary arguments
            if ( arg.equals( BENCHMARK_PHASE_LOAD ) )
            {
                commandlineProperties.put( BENCHMARK_PHASE_ARG, BenchmarkPhase.LOAD_PHASE.toString() );
                argIndex++;
            }
            else if ( arg.equals( BENCHMARK_PHASE_TRANSACTION ) )
            {
                commandlineProperties.put( BENCHMARK_PHASE_ARG, BenchmarkPhase.TRANSACTION_PHASE.toString() );
                argIndex++;
            }
            else if ( arg.equals( SHOW_STATUS_ARG ) )
            {
                commandlineProperties.put( SHOW_STATUS_ARG, "true" );
                argIndex++;
            }
            // Key/Value arguments
            else if ( arg.equals( DB_ARG ) )
            {
                argIndex++;
                if ( argIndex >= args.length )
                {
                    logger.info( usageMessage() );
                    // TODO exit here?
                }
                String argDb = args[argIndex];
                commandlineProperties.put( DB_ARG, argDb );
                argIndex++;
            }
            else if ( arg.equals( THREADS_ARG ) )
            {
                argIndex++;
                if ( argIndex >= args.length )
                {
                    logger.info( usageMessage() );
                    // TODO exit here?
                }
                String argDb = args[argIndex];
                commandlineProperties.put( THREADS_ARG, argDb );
                argIndex++;
            }
            else if ( arg.equals( PROPERTY_FILE_ARG ) )
            {
                argIndex++;
                if ( argIndex >= args.length )
                {
                    logger.info( usageMessage() );
                    // TODO exit here?
                }
                String argPropertiesFile = args[argIndex];
                argIndex++;

                try
                {
                    Properties tempFileProperties = new Properties();
                    tempFileProperties.load( new FileInputStream( argPropertiesFile ) );
                    fileProperties = MapUtils.mergePropertiesToMap( tempFileProperties, fileProperties, true );
                }
                catch ( IOException e )
                {
                    String errMsg = String.format( "Error loading properties file [%s]", argPropertiesFile );
                    logger.error( errMsg, e );
                    throw new ClientException( errMsg, e.getCause() );
                }
            }
            else if ( arg.equals( PROPERTY_ARG ) )
            {
                argIndex++;
                if ( argIndex >= args.length )
                {
                    logger.info( usageMessage() );
                    // TODO exit here?
                }
                int equalsCharPosition = args[argIndex].indexOf( '=' );
                if ( equalsCharPosition < 0 )
                {
                    logger.info( usageMessage() );
                    // TODO exit here?
                }

                String argPropertyName = args[argIndex].substring( 0, equalsCharPosition );
                String argPropertyValue = args[argIndex].substring( equalsCharPosition + 1 );
                commandlineProperties.put( argPropertyName, argPropertyValue );
                argIndex++;
            }
            else
            {
                String errMsg = "Unknown option " + args[argIndex];
                logger.error( errMsg );
                logger.error( usageMessage() );
                throw new ClientException( errMsg );
            }

            if ( argIndex >= args.length )
            {
                break;
            }
        }

        if ( argIndex != args.length )
        {
            logger.error( usageMessage() );
        }

        return MapUtils.mergeMaps( fileProperties, commandlineProperties, false );
    }

    private static Pair<Boolean, String> checkRequiredProperties( Map<String, String> properties,
            String[] requiredProperties )
    {
        for ( String property : requiredProperties )
        {
            if ( false == properties.containsKey( property ) )
            {
                return Pair.create( false, "Missing property: " + property );
            }
        }
        return Pair.create( true, "" );
    }

    private void start( Map<String, String> properties ) throws ClientException
    {
        GeneratorBuilder generatorBuilder = new GeneratorBuilder( new RandomDataGeneratorFactory( RANDOM_SEED ) );

        boolean showStatus = Boolean.parseBoolean( MapUtils.getDefault( properties, SHOW_STATUS_ARG,
                SHOW_STATUS_DEFAULT ) );
        logger.info( String.format( "Show status: %s", showStatus ) );

        int threadCount = Integer.parseInt( MapUtils.getDefault( properties, THREADS_ARG, THREADS_DEFAULT ) );
        logger.info( String.format( "Thread count: %s", threadCount ) );

        BenchmarkPhase benchmarkPhase = BenchmarkPhase.valueOf( MapUtils.getDefault( properties, BENCHMARK_PHASE_ARG,
                BENCHMARK_PHASE_DEFAULT ) );
        logger.info( String.format( "Benchmark phase: %s", benchmarkPhase ) );

        Workload workload = null;
        String workloadName = properties.get( WORKLOAD_ARG );
        try
        {
            workload = ClassLoaderHelper.loadWorkload( workloadName );
            workload.init( properties );
        }
        catch ( Exception e )
        {
            String errMsg = String.format( "Error loading Workload class: %s", workloadName );
            logger.error( errMsg, e );
            throw new ClientException( errMsg, e.getCause() );

        }
        logger.info( String.format( "Loaded Workload: %s", workload.getClass().getName() ) );

        Db db = null;
        String dbName = MapUtils.getDefault( properties, DB_ARG, DB_EXAMPLE );
        try
        {
            db = ClassLoaderHelper.loadDb( dbName );
            db.init( properties );
        }
        catch ( DbException e )
        {
            String errMsg = String.format( "Error loading DB class: %s", dbName );
            logger.error( errMsg, e );
            throw new ClientException( errMsg, e.getCause() );
        }
        logger.info( String.format( "Loaded DB: %s", db.getClass().getName() ) );

        TimeUnit timeUnit = TimeUnit.MILLI;
        TimeUnit durationUnit = TimeUnit.NANO;
        WorkloadMetricsManager metricsManager = new WorkloadMetricsManager( timeUnit, durationUnit );

        int operationCount = getOperationCount( properties, benchmarkPhase );

        WorkloadRunner workloadRunner = new WorkloadRunner( db, benchmarkPhase, workload, operationCount,
                generatorBuilder, showStatus, threadCount, metricsManager );

        logger.info( String.format( "Starting Benchmark (%s operations)", operationCount ) );
        Time startTime = Time.now();
        try
        {
            workloadRunner.run();
        }
        catch ( ClientException e )
        {
            String errMsg = "Error running Workload";
            logger.error( errMsg, e );
            throw new ClientException( errMsg, e.getCause() );
        }
        Time endTime = Time.now();

        logger.info( "Cleaning up Workload..." );
        try
        {
            workload.cleanup();
        }
        catch ( WorkloadException e )
        {
            String errMsg = "Error during Workload cleanup";
            logger.error( errMsg, e );
            throw new ClientException( errMsg, e.getCause() );
        }

        logger.info( "Cleaning up DB..." );
        try
        {
            db.cleanup();
        }
        catch ( DbException e )
        {
            String errMsg = "Error during DB cleanup";
            logger.error( errMsg, e );
            throw new ClientException( errMsg, e.getCause() );
        }

        logger.info( String.format( "Runtime: %s (s)", Duration.durationBetween( startTime, endTime ).asSeconds() ) );
        logger.info( "Exporting Measurements..." );
        try
        {
            MetricsExporter metricsExporter = new OutputStreamMetricsExporter( System.out );
            MetricsFormatter metricsFormatter = new SimpleMetricsFormatter();
            metricsExporter.export( metricsFormatter, metricsManager.getAllMeasurements() );
        }
        catch ( MetricsExporterException e )
        {
            String errMsg = "Could not export Measurements";
            logger.error( errMsg, e );
            throw new ClientException( errMsg, e.getCause() );
        }
    }

    private int getOperationCount( Map<String, String> commandlineProperties, BenchmarkPhase benchmarkPhase )
            throws NumberFormatException
    {
        int operationCount = 0;
        switch ( benchmarkPhase )
        {
        case TRANSACTION_PHASE:
            operationCount = Integer.parseInt( MapUtils.getDefault( commandlineProperties, OPERATION_COUNT_ARG,
                    OPERATION_COUNT_DEFAULT ) );
            break;

        case LOAD_PHASE:
            if ( commandlineProperties.containsKey( INSERT_COUNT_ARG ) )
            {
                operationCount = Integer.parseInt( MapUtils.getDefault( commandlineProperties, INSERT_COUNT_ARG,
                        INSERT_COUNT_DEFAULT ) );
            }
            else
            {
                operationCount = Integer.parseInt( MapUtils.getDefault( commandlineProperties, RECORD_COUNT_ARG,
                        RECORD_COUNT_DEFAULT ) );
            }
            break;
        }
        return operationCount;
    }

    private void exit()
    {
        // TODO YCSB used System.exit(0) to kill its many driver threads. those
        // threads no longer exist, but others will at the DB connection
        // layer. What's the cleanest/safest/right way to terminate the
        // application and clean up all threads?
        System.exit( 0 );
    }
}
