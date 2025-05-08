import type { ReactNode } from 'react';
import Footer from 'konfigyr/components/layout/footer';
import Header from 'konfigyr/components/layout/header';

export type LayoutProps = {
    children: ReactNode | ReactNode[]
}

function Layout({ children }: LayoutProps) {
    return (
        <>
            <Header />
            <main className="items-center justify-center px-4 py-6 lg:py-8">
                {children}
            </main>
            <Footer/>
        </>
    );
}

export {
    Footer,
    Header,
    Layout
};

export default Layout;
