package com.hear2.location.service;

public interface LocationNameResolver {

    ResolvedLocationNames resolve(Double latitude, Double longitude);
}
