package com.team7.cafe.menu.init;

import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.repository.CategoryRepository;
import com.team7.cafe.menu.entity.Menu;
import com.team7.cafe.menu.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MenuDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final MenuRepository menuRepository;

    @Autowired
    public MenuDataInitializer(CategoryRepository categoryRepository, MenuRepository menuRepository) {
        this.categoryRepository = categoryRepository;
        this.menuRepository = menuRepository;
    }

    @Override
    public void run(String... args) {
        // 이미 카테고리나 메뉴 데이터가 존재하면 중복 삽입 방지 [2026.11.03/안형빈]
        if (categoryRepository.count() > 0 || menuRepository.count() > 0) {
            return;
        }

        // 1. 기본 카테고리 생성 (원두 패키지, 드립백, 콜드브루)[2026.11.03/안형빈]
        Category coffeeBeans = categoryRepository.save(new Category("Coffee Beans"));
        Category dripBag = categoryRepository.save(new Category("Drip Bag"));
        Category coldBrew = categoryRepository.save(new Category("Cold Brew"));

        // 2. 초기 메뉴 데이터 생성 (Category, Name, Price, Description)[2026.11.03/안형빈]
        // [원두 패키지]
        menuRepository.save(new Menu(
                coffeeBeans,
                "콜롬비아 수프레모 200g",
                14000,
                "부드러운 산미와 견과류의 고소함이 조화로운 대표 원두 패키지"
        ));
        menuRepository.save(new Menu(
                coffeeBeans,
                "에티오피아 예가체프 G2 200g",
                16000,
                "화사한 꽃향기와 은은한 과일의 단맛을 머금은 원두 패키지"
        ));
        menuRepository.save(new Menu(
                coffeeBeans,
                "과테말라 안티구아 200g",
                15000,
                "스모키한 풍미와 깊고 묵직한 바디감이 특징인 원두 패키지"
        ));

        // [드립백]
        menuRepository.save(new Menu(
                dripBag,
                "시그니처 블렌드 드립백 5개입",
                8500,
                "언제 어디서나 간편하게 즐기는 풍부한 밸런스의 드립백 세트"
        ));

        // [콜드브루]
        menuRepository.save(new Menu(
                coldBrew,
                "에티오피아 싱글오리진 콜드브루 500ml",
                12000,
                "저온 추출로 깔끔하고 산뜻한 과실향을 극대화한 원액"
        ));
    }
}