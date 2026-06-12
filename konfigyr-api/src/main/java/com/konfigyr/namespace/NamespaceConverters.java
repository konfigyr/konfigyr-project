package com.konfigyr.namespace;

import com.konfigyr.data.converter.JsonbConverter;
import com.konfigyr.security.NamespaceApplicationSettings;
import com.konfigyr.security.NamespaceClientId;
import com.konfigyr.security.NamespaceClientType;
import org.jooq.Converter;
import org.jooq.JSONB;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@NullMarked
final class NamespaceConverters {

	private final Converter<JSONB, NamespaceApplicationSettings> settingsConverter;
	private final Converter<String, NamespaceClientType> clientTypeConverter;
	private final Converter<String, NamespaceClientId> clientIdConverter;
	private final Converter<JSONB, List<String>> stringListConverter;
	private final Converter<JSONB, Map<String, String>> stringMapConverter;

	NamespaceConverters(JsonMapper mapper) {
		this.settingsConverter = JsonbConverter.create(mapper, NamespaceApplicationSettings.class);
		this.clientTypeConverter = Converter.of(
				String.class, NamespaceClientType.class, NamespaceClientType::valueOf, NamespaceClientType::name
		);
		this.clientIdConverter = Converter.of(
				String.class, NamespaceClientId.class, NamespaceClientId::parse, NamespaceClientId::get
		);
		this.stringListConverter = JsonbConverter.create(mapper,
				mapper.getTypeFactory().constructCollectionType(List.class, String.class));
		this.stringMapConverter = JsonbConverter.create(mapper,
				mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
	}

	Converter<String, NamespaceClientId> clientId() {
		return clientIdConverter;
	}

	Converter<String, NamespaceClientType> clientType() {
		return clientTypeConverter;
	}

	Converter<JSONB, NamespaceApplicationSettings> settings() {
		return settingsConverter;
	}

	Converter<JSONB, List<String>> stringList() {
		return stringListConverter;
	}

	Converter<JSONB, Map<String, String>> stringMap() {
		return stringMapConverter;
	}

}
