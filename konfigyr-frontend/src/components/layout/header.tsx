import Link from 'next/link';
import ServiceLabel from 'konfigyr/components/service-label';

export default function Header() {
    return (
        <header id="header" className="border-b">
            <div className="container px-4 flex h-14 items-center gap-4">
                <Link href="/" title="Konfigyr Vault" className="text-2xl">
                    <ServiceLabel />
                </Link>

                <div className="flex flex-1 items-center justify-end">
                    <a href="/auth/authorize">
                        Login
                    </a>
                </div>
            </div>
        </header>
    );
}
