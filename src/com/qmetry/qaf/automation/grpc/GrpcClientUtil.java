/**
 * 
 */
package com.qmetry.qaf.automation.grpc;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONException;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.qmetry.qaf.automation.core.AutomationError;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.LoggingBean;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.qmetry.qaf.automation.util.FileUtil;
import com.qmetry.qaf.automation.util.JSONUtil;
import com.qmetry.qaf.automation.util.StringUtil;

import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import me.dinowernli.grpc.polyglot.command.ServiceCall;
import me.dinowernli.grpc.polyglot.command.ServiceList;
import me.dinowernli.grpc.polyglot.config.ConfigurationLoader;
import me.dinowernli.grpc.polyglot.io.Output;
import me.dinowernli.grpc.polyglot.protobuf.ProtocInvoker;
import me.dinowernli.grpc.polyglot.protobuf.ProtocInvoker.ProtocInvocationException;
import me.dinowernli.grpc.polyglot.protobuf.ServiceResolver;
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

			if (StringUtil.isBlank(request.getBody())) {
				request.setBody("{}");
			}

			if (StringUtil.isBlank(request.getProto_discovery_root())) {
				String protoFilePath = getProtoFilePath(request.getEndPoint());
				if(StringUtil.isNotBlank(protoFilePath)) {
					request.setProto_discovery_root(protoFilePath);
				}else {
					request.setProto_discovery_root(getBundle().getString("grpc.proto_discovery_root"));
				}
			}
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

			ProtoConfiguration protoConfig = config.getProtoConfig();
			try (PrintStream ps = new PrintStream(baos, true, utf8)) {
				Output output = Output.forStream(ps);
				long stTime = System.currentTimeMillis();
				Long duration = 0l;

				try {
					ServiceCall.callEndpoint(output, protoConfig, Optional.of(request.getBaseUrl()),
							Optional.of(request.getEndPoint()),
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

	public static GrpcResponse getGrpcResponse() {
		return (GrpcResponse) TestBaseProvider.instance().get().getContext().getProperty(GRPC_RESPONSE_KEY);
	}
	
	public static List<MethodDescriptor> listMethods(String protoDiscoveryRoot) throws ProtocInvocationException {

		final ConfigurationLoader configLoader = maybeInputPath(
				getBundle().getString("grpc.config_set_path", "resources/proto/grpc.config.json"))
				.map(ConfigurationLoader::forFile).orElseGet(() -> ConfigurationLoader.forDefaultConfigSet());// .withOverrides(arguments
																												// )

		Builder configBuilder = Optional.ofNullable(getBundle().getString("grpc.config_name"))
				.map(configLoader::getNamedConfiguration).orElseGet(() -> configLoader.getDefaultConfiguration())
				.toBuilder();
		configBuilder.getProtoConfigBuilder().setProtoDiscoveryRoot(protoDiscoveryRoot);

		Configuration config = configBuilder.build();

		FileDescriptorSet fileDescriptorSet = getFileDescriptorSet(config.getProtoConfig());

		ServiceResolver serviceResolver = ServiceResolver.fromFileDescriptorSet(fileDescriptorSet);
		List<MethodDescriptor> methods = new ArrayList<>();
		for (ServiceDescriptor descriptor : serviceResolver.listServices()) {

			try {
				methods.addAll(descriptor.getMethods());
			} catch (Throwable e) {
				e.printStackTrace();
				System.err.println("ERROR:: " + descriptor.getFullName());
			}
		}
		return methods;
	}
	public static String renderDescriptor(Descriptor descriptor, String indent) {
		if (descriptor.getFields().size() == 0) {
			return indent + "<empty>";
		}

		List<String> fieldsAsStrings = descriptor.getFields().stream()
				.map(field -> renderDescriptor(field, indent + "  ")).collect(Collectors.toList());

		return Joiner.on(System.lineSeparator()).join(fieldsAsStrings);
	}

	private static String renderDescriptor(FieldDescriptor descriptor, String indent) {
		String isOpt = descriptor.isOptional() ? "<optional>" : "<required>";
		String isRep = descriptor.isRepeated() ? "<repeated>" : "<single>";
		String fieldPrefix = indent + descriptor.getJsonName() + "[" + isOpt + " " + isRep + "]";

		if (descriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
			return fieldPrefix + " {" + System.lineSeparator()
					+ renderDescriptor(descriptor.getMessageType(), indent + "  ") + System.lineSeparator() + indent
					+ "}";

		} else if (descriptor.getJavaType() == FieldDescriptor.JavaType.ENUM) {
			return fieldPrefix + ": " + descriptor.getEnumType().getValues();

		} else {
			return fieldPrefix + ": " + descriptor.getJavaType();
		}
	}
	public static void list() {
		try {
			final ConfigurationLoader configLoader = maybeInputPath(getBundle().getString("grpc.config_set_path"))
					.map(ConfigurationLoader::forFile).orElseGet(() -> ConfigurationLoader.forDefaultConfigSet());// .withOverrides(arguments);

			Builder configBuilder = Optional.ofNullable(getBundle().getString("grpc.config_name"))
					.map(configLoader::getNamedConfiguration).orElseGet(() -> configLoader.getDefaultConfiguration())
					.toBuilder();
			configBuilder.getProtoConfigBuilder()
					.setProtoDiscoveryRoot(getBundle().getString("grpc.proto_discovery_root"));
			Configuration config = configBuilder.build();
			ProtoConfiguration protoConfig = config.getProtoConfig();
				Output output = Output.forStream(new PrintStream(System.out) {public void print(String s) {super.print("Start::" +s);};});

				try {
			        FileDescriptorSet fileDescriptorSet = getFileDescriptorSet(config.getProtoConfig());
					ServiceList.listServices(output, fileDescriptorSet, protoConfig.getProtoDiscoveryRoot(), Optional.ofNullable(null), Optional.ofNullable(null), Optional.ofNullable(true));
				} catch (Exception e) {
					e.printStackTrace();
				}

		} catch (Exception | Error e) {
			throw new AutomationError(e);
		}
	}
	
	public static void list(String file) {
		
		try {
			final ConfigurationLoader configLoader = maybeInputPath(getBundle().getString("grpc.config_set_path"))
					.map(ConfigurationLoader::forFile).orElseGet(() -> ConfigurationLoader.forDefaultConfigSet());// .withOverrides(arguments);

			Builder configBuilder = Optional.ofNullable(getBundle().getString("grpc.config_name"))
					.map(configLoader::getNamedConfiguration).orElseGet(() -> configLoader.getDefaultConfiguration())
					.toBuilder();
			configBuilder.getProtoConfigBuilder()
					.setProtoDiscoveryRoot(getBundle().getString("grpc.proto_discovery_root",new File(file).getParentFile().getPath()));
			Configuration config = configBuilder.build();

			ProtoConfiguration protoConfig = config.getProtoConfig();
				Output output = Output.forStream(new PrintStream(System.out) {public void print(String s) {super.print("Start::" +s);};});

				try {
			        FileDescriptorSet fileDescriptorSet = getFileDescriptorSet(config.getProtoConfig());
					ServiceList.listServices(output, fileDescriptorSet, protoConfig.getProtoDiscoveryRoot(), Optional.ofNullable(null), Optional.ofNullable(null), Optional.ofNullable(true));
				} catch (Exception e) {
					e.printStackTrace();
				}
		} catch (Exception | Error e) {
			throw new AutomationError(e);
		}
	}

	 /** Invokes protoc and returns a {@link FileDescriptorSet} used for discovery. 
	 * @throws ProtocInvocationException */
	  private static FileDescriptorSet getFileDescriptorSet(ProtoConfiguration protoConfig) throws ProtocInvocationException {
	    ProtocInvoker invoker = ProtocInvoker.forConfig(protoConfig);
	    return invoker.invoke();
	  }

	private static Optional<Path> maybeOutputPath(String rawPath) {
		if (rawPath == null) {
			return Optional.empty();
		}
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
	
	private static String getProtoFilePath(String endpoint) {
		String methodDescriptorKey="grpcmethod."+endpoint.replace("/", ".").trim();

		String methodDescriptor=getBundle().getString(methodDescriptorKey);
		if(StringUtil.isBlank(methodDescriptor)) {
    		String root = ConfigurationManager.getBundle().getString("grpc.proto_discovery_root", "resources/proto");
    		File discoveryFile = new File(root,"proto-discovery.properties");

    		if(discoveryFile.exists()) {
    			ConfigurationManager.getBundle().load(new String[] {discoveryFile.getPath()});
				methodDescriptor=getBundle().getString(methodDescriptorKey);
    		}
    		if(StringUtil.isBlank(methodDescriptor)) {
    			Collection<File> allProtoFiles = FileUtil.getFiles(new File(root), "proto", true); 
    			PropertiesConfiguration prop = new PropertiesConfiguration();
    			prop.setEncoding("UTF-8");

    			//Properties prop = new Properties();
    			try(FileInputStream fis = new FileInputStream(discoveryFile)) {
					prop.load(fis);
				} catch (IOException | ConfigurationException e1) {
				}
    			for(File protoFile : allProtoFiles) {
					try {
						List<MethodDescriptor> methods = listMethods(protoFile.getPath());
						for(MethodDescriptor m:methods) {
							int lastDot = m.getFullName().lastIndexOf('.');
							StringBuilder methodName = new StringBuilder(m.getFullName());
							methodName.replace(lastDot, lastDot+1, "/");
							prop.setProperty("grpcmethod." +m.getFullName(), String.format("{'endpoint':'%s','proto_discovery_root':'%s'}",methodName, protoFile.getPath()));
						}
					} catch (ProtocInvocationException e) {
					}
    			}
    			try(FileOutputStream out = new FileOutputStream(discoveryFile)) {
					prop.save(out, "UTF-8");
				} catch (IOException | ConfigurationException e) {
					e.printStackTrace();
				}
    			ConfigurationManager.getBundle().load(new String[] {discoveryFile.getPath()});
				methodDescriptor=getBundle().getString(methodDescriptorKey);
    		}
		}
		if(StringUtil.isNotBlank(methodDescriptor)) {
			try {
				return (String) JSONUtil.toMap(methodDescriptor).getOrDefault("proto_discovery_root","");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return "";
	}

}
