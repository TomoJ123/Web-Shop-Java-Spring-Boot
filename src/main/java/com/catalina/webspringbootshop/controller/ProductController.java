package com.catalina.webspringbootshop.controller;

import com.catalina.webspringbootshop.dto.ProductForm;
import com.catalina.webspringbootshop.entity.CartItem;
import com.catalina.webspringbootshop.entity.Category;
import com.catalina.webspringbootshop.entity.Product;
import com.catalina.webspringbootshop.entity.User;
import com.catalina.webspringbootshop.repository.CategoryRepository;
import com.catalina.webspringbootshop.repository.ProductRepository;
import com.catalina.webspringbootshop.repository.UserRepository;
import com.catalina.webspringbootshop.service.CartService;
import com.catalina.webspringbootshop.service.ProductService;
import com.catalina.webspringbootshop.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProductController {
    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryRepository categoryrepo;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private final double TAX = 0.2533;

    private final ProductService productService;
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductRepository productRepository, ProductService productService, CategoryRepository categoryRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }


    @GetMapping(value = {"/"})
    public String dashboard(ModelMap model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userRepository.findByUsername(userDetails.getUsername());

        List<CartItem> cartItems = cartService.listCartItems(user);
        double cartSum = cartItems.stream().mapToDouble(o -> o.getProduct().getPrice()).sum();
        double totalCartSum = Math.floor((cartSum + cartSum * TAX) * 100) / 100;
        int totalCartItems = cartItems.stream().mapToInt(el -> el.getQuantity()).sum();
        List<Integer> productIds = cartItems.stream().map(i -> i.getProduct().getId()).collect(Collectors.toList());

        model.addAttribute("products", getAllProducts());
        model.addAttribute("categories", listAllCategories());
        model.addAttribute("userDetails", userDetails);
        model.addAttribute("totalCartSum", totalCartSum);
        model.addAttribute("productIds", productIds);
        model.addAttribute("totalCartItems", totalCartItems);

        return "index";
    }

    @GetMapping(value = {"/products"})
    public String index(ModelMap model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername());
        List<CartItem> cartItems = cartService.listCartItems(user);
        double cartSum = cartItems.stream().mapToDouble(o -> o.getProduct().getPrice()).sum();
        double totalCartSum = Math.floor((cartSum + cartSum * TAX) * 100) / 100;
        int totalCartItems = cartItems.stream().mapToInt(el -> el.getQuantity()).sum();
        List<Integer> productIds = cartItems.stream().map(i -> i.getProduct().getId()).collect(Collectors.toList());

        model.addAttribute("products", getAllProducts());
        model.addAttribute("categories", listAllCategories());
        model.addAttribute("userDetails", userDetails);
        model.addAttribute("totalCartSum", totalCartSum);
        model.addAttribute("productIds", productIds);
        model.addAttribute("totalCartItems", totalCartItems);

        return "products";
    }

    @GetMapping(value = {"/{id}"})
    public String get(@PathVariable("id") int id, ModelMap model) {
        model.addAttribute("products", productRepository.findById(id));

        return "product";
    }

    @RequestMapping(value = "/admin/product/new", method = {RequestMethod.POST, RequestMethod.GET})
    public String newProduct(ModelMap model, ProductForm product, HttpServletRequest req, RedirectAttributes attr) {
        if (StringUtils.equals(req.getMethod(), RequestMethod.GET.toString())) {
            model.addAttribute("productForm", new Product());
            model.addAttribute("categories", categoryrepo.findAll() );

            return "create-product";
        }


        if (StringUtils.equals(req.getMethod(), RequestMethod.POST.toString())) {
            Category newCategory = categoryRepository.findById(product.getCategory().getId());
            Product newProduct = new Product(product.getName(), product.getPrice(), product.getUnit_in_stock(),
                    product.getDescription() ,newCategory);

            productService.save(newProduct);
            logger.debug(String.format("Product with id: %s successfully created.", newProduct.getId()));

            return "redirect:/";
        }

        return invalidRequestResponse(attr);
    }

    @RequestMapping(value = "/product/edit/{id}", method = {RequestMethod.POST})
    public String update(@PathVariable("id") int id, Product product, HttpServletRequest req, RedirectAttributes attr) {
        if (StringUtils.equals(req.getMethod(), RequestMethod.POST.toString())) {
            productService.edit(id, product);
            logger.debug(String.format("Product with id: %s has been successfully edited.", id));

            return "redirect:/";
        }

        return invalidRequestResponse(attr);
    }

    @RequestMapping(value = "/product/delete/{id}", method = {RequestMethod.POST})
    public String destroy(@PathVariable("id") int productId) {
        Product product = productService.findById(productId);

        if (product != null) {
            productService.delete(productId);
            logger.debug(String.format("Product with id: %s successfully deleted.", product.getId()));
            return "redirect:/";
        }

        return "error/404";
    }

    private List<Product> getAllProducts() {
        return productService.findAllByOrderByAsc();
    }


    public List<Category> listAllCategories() {
        return categoryRepository.findAll();
    }

    private String invalidRequestResponse(RedirectAttributes attr) {
        attr.addFlashAttribute("error", "Invalid Request Method");
        return "redirect:/";
    }
}
