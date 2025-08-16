package kuke.board.articleread.cache;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class OptimizedCacheAspect {
    private final OptimizedCacheManager optimizedCacheManager;

    /**
     * 흐름 정리
     * 원본 메서드에 @OptimizedCacheable 붙으면, AOP가 가로챔
     * around()가 실행되고,
     * optimizedCacheManager.process()에 람다 () -> joinPoint.proceed()를 넘겨줌
     * process()는 캐시에서 데이터 조회 후, 필요하면 람다(originDataSupplier.get())를 호출해서 실제 원본 메서드 실행
     * 실행된 결과를 캐시에 저장하고 반환
     */
    @Around("@annotation(OptimizedCacheable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        OptimizedCacheable cacheable = findAnnotation(joinPoint);
        return optimizedCacheManager.process(
                cacheable.type(),
                cacheable.ttlSeconds(),
                joinPoint.getArgs(),
                findReturnType(joinPoint),
                // 이 람다 자체가 OptimizedCacheOriginDataSupplier의 get() 메서드 구현이고,
                // joinPoint.proceed() = 현재 AOP가 가로챈 원본 메서드를 실제로 호출하는 코드
                // joinPoint.proceed()가 원본 메서드(실제 캐시 대상 메서드)를 호출하는 부분
                // joinPoint.proceed() 호출 시점은 optimizedCacheManager.process() 메서드 내부에서 결정됨
                () -> joinPoint.proceed()
        );
    }

    private OptimizedCacheable findAnnotation(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        return methodSignature.getMethod().getAnnotation(OptimizedCacheable.class);
    }

    /**
     * 캐시에서 꺼낸 데이터를 원래 메서드가 반환하는 타입으로 역직렬화(deserialize) 하거나
     * 반환 타입에 맞게 타입 캐스팅할 때 사용
     */
    private Class<?> findReturnType(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        return methodSignature.getReturnType();
    }
}
