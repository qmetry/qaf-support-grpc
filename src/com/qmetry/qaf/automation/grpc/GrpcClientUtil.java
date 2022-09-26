/**
 * 
 */
package com.qmetry.qaf.automation.grpc;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.qmetry.qaf.automation.core.AutomationError;
import com.qmetry.qaf.automation.core.LoggingBean;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.qmetry.qaf.automation.util.StringUtil;

import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import me.dinowernli.grpc.polyglot.command.ServiceCall;
import me.dinowernli.grpc.polyglot.command.ServiceList;
import me.dinowernli.grpc.polyglot.config.ConfigurationLoader;
import me.dinowernli.grpc.polyglot.io.Output;
import me.dinowernli.grpc.polyglot.protobuf.ProtocInvoker;
import me.dinowernli.grpc.polyglot.protobuf.ProtocInvoker.ProtocInvocationException;
import polyglot.ConfigProto.Configuration;
import polyglot.ConfigProto.Configuration.Builder;
import polyglot.ConfigProto.ProtoConfiguration;

/**
 * @author chirag.jayswal
 *
 */
public class GrpcClientUtil {
	public static final String GRPC_RESPONSE_KEY = "_grpc.response";

	public static GrpcResponse call(GrpcRequestBean request) {
		TestBaseProvider.instance().get().getContext().clearProperty(GRPC_RESPONSE_KEY);
		GrpcResponse response = null;
		String message = "";
		try {

//			if (StringUtil.isBlank(request.getBody())) {
//				request.setBody("{}");
//			}
			final byte[] payload = request.getBody().getBytes();
			final ByteArrayInputStream payloadStream = new ByteArrayInputStream(payload);
			System.setIn(payloadStream);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final String utf8 = StandardCharsets.UTF_8.name();
			
			String proto_discovery_root = request.getProto_discovery_root();
			String config_set_path = request.getConfig_set_path();
			String config_name = getBundle().getString("grpc.config_name");

			final ConfigurationLoader configLoader = maybeInputPath(config_set_path)
					.map(ConfigurationLoader::forFile).orElseGet(() -> ConfigurationLoader.forDefaultConfigSet());// .withOverrides(arguments);

			Builder configBuilder = Optional.ofNullable(config_name)
					.map(configLoader::getNamedConfiguration).orElseGet(() -> configLoader.getDefaultConfiguration())
					.toBuilder();
			configBuilder.getProtoConfigBuilder()
					.setProtoDiscoveryRoot(proto_discovery_root);
			Configuration config = configBuilder.build();
			// Output output = Output.forConfiguration(config.getOutputConfig());

			ProtoConfiguration protoConfig = config.getProtoConfig();
			try (PrintStream ps = new PrintStream(baos, true, utf8)) {
				Output output = Output.forStream(ps);
				long stTime = System.currentTimeMillis();
				Long duration = 0l;

				try {
					ServiceCall.callEndpoint(output, protoConfig, Optional.of(request.getBaseUrl()),
							Optional.of(request.getQualifiedMethod()),
							maybeInputPath(proto_discovery_root),
							maybeInputPath(config_set_path), ImmutableList.of(),
							config.getCallConfig());
					duration = (System.currentTimeMillis() - stTime);

					message = baos.toString(utf8);
					response = new GrpcResponse(message);
				} catch (Exception e) {
					duration = (System.currentTimeMillis() - stTime);
					Throwable cause = causeFromThrowable(e);
					if (baos.size() <= 0) {
						cause.printStackTrace(ps);
					}
					message = baos.toString(utf8);
					response = new GrpcResponse(message, causeFromThrowable(e));
				}

				TestBaseProvider.instance().get().getContext().setProperty(GRPC_RESPONSE_KEY, response);

				// add command log
				String status = response.getStatus().getCode().name();
				if (StringUtil.isNotBlank(response.getStatus().getDescription())) {
					status = status + " : " + response.getStatus().getDescription();
				}
				LoggingBean requestLog = new LoggingBean("grpc://" + request.getBaseUrl(),
						new String[] { request.getEndPoint() }, status);

				LoggingBean respLog = new LoggingBean("", new String[] { request.getBody() }, message);
				respLog.setDuration(duration.intValue());
				requestLog.getSubLogs().add(respLog);

				TestBaseProvider.instance().get().getLog().add(requestLog);

			} 
		} catch (Exception | Error e) {
			throw new AutomationError(e);
		}
		return response;
	}

	public static GrpcResponse getResponse() {
		return (GrpcResponse) TestBaseProvider.instance().get().getContext().getProperty(GRPC_RESPONSE_KEY);
	}
	
	public static void list() {
		
		try {
			long stTime=System.currentTimeMillis();
			


			final ConfigurationLoader configLoader = maybeInputPath(getBundle().getString("grpc.config_set_path"))
					.map(ConfigurationLoader::forFile).orElseGet(() -> ConfigurationLoader.forDefaultConfigSet());// .withOverrides(arguments);

			Builder configBuilder = Optional.ofNullable(getBundle().getString("grpc.config_name"))
					.map(configLoader::getNamedConfiguration).orElseGet(() -> configLoader.getDefaultConfiguration())
					.toBuilder();
			configBuilder.getProtoConfigBuilder()
					.setProtoDiscoveryRoot(getBundle().getString("grpc.proto_discovery_root"));
			Configuration config = configBuilder.build();
			// Output output = Output.forConfiguration(config.getOutputConfig());

			ProtoConfiguration protoConfig = config.getProtoConfig();
				Output output = Output.forStream(new PrintStream(System.out) {public void print(String s) {super.print("Start::" +s);};});

				try {
			          FileDescriptorSet fileDescriptorSet = getFileDescriptorSet(config.getProtoConfig());

					ServiceList.listServices(output, fileDescriptorSet, protoConfig.getProtoDiscoveryRoot(), Optional.ofNullable(null), Optional.ofNullable(null), Optional.ofNullable(true));
//			          ServiceList.listServices(
//			              commandLineOutput,
//			              fileDescriptorSet, config.getProtoConfig().getProtoDiscoveryRoot(),
//			              arguments.serviceFilter(), arguments.methodFilter(), arguments.withMessage());
				} catch (Exception e) {
					e.printStackTrace();
				}
			System.err.println("Time Taken: " + (System.currentTimeMillis() - stTime));

		} catch (Exception | Error e) {
			throw new AutomationError(e);
		}
	}
	
	public static void list(String file) {
		
		try {
			long stTime=System.currentTimeMillis();
			


			final ConfigurationLoader configLoader = maybeInputPath(getBundle().getString("grpc.config_set_path"))
					.map(ConfigurationLoader::forFile).orElseGet(() -> ConfigurationLoader.forDefaultConfigSet());// .withOverrides(arguments);

			Builder configBuilder = Optional.ofNullable(getBundle().getString("grpc.config_name"))
					.map(configLoader::getNamedConfiguration).orElseGet(() -> configLoader.getDefaultConfiguration())
					.toBuilder();
			configBuilder.getProtoConfigBuilder()
					.setProtoDiscoveryRoot(getBundle().getString("grpc.proto_discovery_root",new File(file).getParentFile().getPath()));
			Configuration config = configBuilder.build();
			// Output output = Output.forConfiguration(config.getOutputConfig());

			ProtoConfiguration protoConfig = config.getProtoConfig();
				Output output = Output.forStream(new PrintStream(System.out) {public void print(String s) {super.print("Start::" +s);};});

				try {
			          FileDescriptorSet fileDescriptorSet = getFileDescriptorSet(config.getProtoConfig());

					ServiceList.listServices(output, fileDescriptorSet, protoConfig.getProtoDiscoveryRoot(), Optional.ofNullable(null), Optional.ofNullable(null), Optional.ofNullable(true));
//			          ServiceList.listServices(
//			              commandLineOutput,
//			              fileDescriptorSet, config.getProtoConfig().getProtoDiscoveryRoot(),
//			              arguments.serviceFilter(), arguments.methodFilter(), arguments.withMessage());
				} catch (Exception e) {
					e.printStackTrace();
				}
			System.err.println("Time Taken: " + (System.currentTimeMillis() - stTime));

		} catch (Exception | Error e) {
			throw new AutomationError(e);
		}
	}

	 /** Invokes protoc and returns a {@link FileDescriptorSet} used for discovery. */
	  private static FileDescriptorSet getFileDescriptorSet(ProtoConfiguration protoConfig) {
	    try {
	    	ProtocInvoker invoker = ProtocInvoker.forConfig(protoConfig);
	      return invoker.invoke();
	    } catch (ProtocInvocationException e) {
	      throw new RuntimeException("Failed to invoke the protoc binary", e);
	    }
	  }

	private static Optional<Path> maybeOutputPath(String rawPath) {
		if (rawPath == null) {
			return Optional.empty();
		}
		// Path path = Paths.get(rawPath);
		return Optional.of(Paths.get(rawPath));
	}

	private static Optional<Path> maybeInputPath(String rawPath) {
		return maybeOutputPath(rawPath).map(path -> {
			Preconditions.checkArgument(Files.exists(path), "File " + rawPath + " does not exist");
			return path;
		});
	}

	private static Throwable causeFromThrowable(Throwable t) {
		Throwable cause = t.getCause() == null ? t : t.getCause();
		while (cause != null) {
			if (cause instanceof StatusException || cause instanceof StatusRuntimeException) {
				return cause;
			}
			cause = cause.getCause();
		}
		// Couldn't find a cause with a Status
		return t;
	}

}
